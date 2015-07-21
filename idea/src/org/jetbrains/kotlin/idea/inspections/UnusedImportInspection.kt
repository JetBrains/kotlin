/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonListeners
import com.intellij.codeInsight.daemon.impl.HighlightingSessionImpl
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.DocumentUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.KotlinImportOptimizer
import org.jetbrains.kotlin.idea.imports.importableFqNameSafe
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import java.util.ArrayList
import java.util.HashSet

class UnusedImportInspection : AbstractKotlinInspection() {
    override fun runForWholeFile() = true

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (file !is JetFile) return null
        if (file.importDirectives.isEmpty()) return null

        val descriptorsToImport = KotlinImportOptimizer.collectDescriptorsToImport(file)

        val fqNames = HashSet<FqName>()
        val parentFqNames = HashSet<FqName>()
        for (descriptor in descriptorsToImport) {
            val fqName = descriptor.importableFqNameSafe
            fqNames.add(fqName)
            val parentFqName = fqName.parent()
            if (!parentFqName.isRoot) {
                parentFqNames.add(parentFqName)
            }
        }

        val problems = ArrayList<ProblemDescriptor>()
        val directives = file.importDirectives
        for (directive in directives) {
            val importPath = directive.importPath ?: continue
            if (importPath.alias != null) continue // highlighting of unused alias imports not supported yet
            val isUsed = if (importPath.isAllUnder) {
                importPath.fqnPart() in parentFqNames
            }
            else {
                importPath.fqnPart() in fqNames
            }

            if (!isUsed) {
                val nameExpression = directive.importedReference?.getQualifiedElementSelector() as? JetSimpleNameExpression
                if (nameExpression == null || nameExpression.getReferenceTargets(nameExpression.analyze()).isEmpty()) continue // do not highlight unresolved imports as unused

                problems.add(manager.createProblemDescriptor(directive,
                                                             "Unused import directive",
                                                             isOnTheFly,
                                                             arrayOf(OptimizeImportsQuickFix(file)),
                                                             ProblemHighlightType.LIKE_UNUSED_SYMBOL))
            }
        }

        scheduleOptimizeImportsOnTheFly(file, descriptorsToImport)

        return problems.toTypedArray()
    }

    private fun scheduleOptimizeImportsOnTheFly(file: JetFile, descriptorsToImport: Set<DeclarationDescriptor>) {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            val optimizedImports = KotlinImportOptimizer.prepareOptimizedImports(file, descriptorsToImport) ?: return // return if already optimized

            // unwrap progress indicator
            val progress = sequence(ProgressManager.getInstance().progressIndicator) {
                (it as? ProgressWrapper)?.originalProgressIndicator
            }.last()
            val highlightingSession = HighlightingSessionImpl.getHighlightingSession(file, progress)

            val project = highlightingSession.project
            val editor = highlightingSession.editor
            if (editor != null) {
                Disposer.register(highlightingSession, object : Disposable {
                    override fun dispose() {
                        // later because should invoke when highlighting is finished
                        ApplicationManager.getApplication().invokeLater {
                            if (timeToOptimizeImportsOnTheFly(file, editor, project)) {
                                optimizeImportsOnTheFly(file, optimizedImports, editor, project)
                            }
                        }
                    }
                })
            }
        }
    }

    private fun timeToOptimizeImportsOnTheFly(file: JetFile, editor: Editor, project: Project): Boolean {
        if (project.isDisposed || !file.isValid || editor.isDisposed) return false

        if (!file.isWritable) return false

        // do not optimize imports on the fly during undo/redo
        val undoManager = UndoManager.getInstance(editor.project)
        if (undoManager.isUndoInProgress || undoManager.isRedoInProgress) return false

        // if we stand inside import statements, do not optimize
        val importsRange = file.importList?.textRange ?: return false
        if (importsRange.containsOffset(editor.caretModel.offset)) return false

        val codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project)
        if (!codeAnalyzer.isHighlightingAvailable(file)) return false
        if (!codeAnalyzer.isErrorAnalyzingFinished(file)) return false

        val document = editor.document
        var hasErrors = false
        DaemonCodeAnalyzerEx.processHighlights(document, project, HighlightSeverity.ERROR, 0, document.textLength, Processor { hasErrors = true; false })
        if (hasErrors) return false

        return DaemonListeners.canChangeFileSilently(file)
    }

    private fun optimizeImportsOnTheFly(file: JetFile, optimizedImports: List<ImportPath>, editor: Editor, project: Project) {
        PsiDocumentManager.getInstance(file.project).commitAllDocuments()
        DocumentUtil.writeInRunUndoTransparentAction {
            KotlinImportOptimizer.replaceImports(file, optimizedImports)
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        }
    }

    private class OptimizeImportsQuickFix(private val file: JetFile): LocalQuickFix {
        override fun getName() = "Optimize imports"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            OptimizeImportsProcessor(project, file).run()
        }
    }

}