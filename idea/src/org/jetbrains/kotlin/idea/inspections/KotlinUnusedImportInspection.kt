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
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonListeners
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInsight.daemon.impl.HighlightingSessionImpl
import com.intellij.codeInsight.intention.LowPriorityAction
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
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.imports.KotlinImportOptimizer
import org.jetbrains.kotlin.idea.imports.OptimizedImportsBuilder
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.*

class KotlinUnusedImportInspection : AbstractKotlinInspection() {
    override fun runForWholeFile() = true

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (file !is KtFile || file is KtCodeFragment) return null
        if (!file.manager.isInProject(file)) return null
        if (file.importDirectives.isEmpty()) return null

        val data = KotlinImportOptimizer.collectDescriptorsToImport(file)

        val directives = file.importDirectives
        val explicitlyImportedFqNames = directives
                .asSequence()
                .mapNotNull { it.importPath }
                .filter { !it.isAllUnder && !it.hasAlias() }
                .map { it.fqnPart() }
                .toSet()

        val fqNames = HashSet<FqName>()
        val parentFqNames = HashSet<FqName>()
        for (descriptor in data.descriptorsToImport) {
            val fqName = descriptor.importableFqName!!
            fqNames.add(fqName)

            if (fqName !in explicitlyImportedFqNames) { // we don't add parents of explicitly imported fq-names because such imports are not needed
                val parentFqName = fqName.parent()
                if (!parentFqName.isRoot) {
                    parentFqNames.add(parentFqName)
                }
            }
        }

        val problems = ArrayList<ProblemDescriptor>()

        val importPaths = HashSet<ImportPath>(directives.size)

        for (directive in directives) {
            val importPath = directive.importPath ?: continue
            if (importPath.alias != null) continue // highlighting of unused alias imports not supported yet

            val isUsed = if (!importPaths.add(importPath)) {
                false
            }
            else if (importPath.isAllUnder) {
                importPath.fqnPart() in parentFqNames
            }
            else {
                importPath.fqnPart() in fqNames
            }

            if (!isUsed) {
                if (directive.targetDescriptors().isEmpty()) continue // do not highlight unresolved imports as unused

                val fixes = arrayListOf<LocalQuickFix>()
                fixes.add(OptimizeImportsQuickFix(file))
                if (!CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
                    fixes.add(EnableOptimizeImportsOnTheFlyFix(file))
                }

                problems.add(manager.createProblemDescriptor(directive,
                                                             "Unused import directive",
                                                             isOnTheFly,
                                                             fixes.toTypedArray(),
                                                             ProblemHighlightType.LIKE_UNUSED_SYMBOL))
            }
        }

        if (isOnTheFly) {
            scheduleOptimizeImportsOnTheFly(file, data)
        }

        return problems.toTypedArray()
    }

    private fun scheduleOptimizeImportsOnTheFly(file: KtFile, data: OptimizedImportsBuilder.InputData) {
        if (!CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) return
        val optimizedImports = KotlinImportOptimizer.prepareOptimizedImports(file, data) ?: return // return if already optimized

        // unwrap progress indicator
        val progress = generateSequence(ProgressManager.getInstance().progressIndicator) {
            (it as? ProgressWrapper)?.originalProgressIndicator
        }.last() as DaemonProgressIndicator
        val highlightingSession = HighlightingSessionImpl.getHighlightingSession(file, progress)

        val project = highlightingSession.project
        val editor = highlightingSession.editor
        if (editor != null) {
            val modificationStamp = editor.document.modificationStamp
            val invokeFixLater = object : Disposable {
                override fun dispose() {
                    // later because should invoke when highlighting is finished
                    ApplicationManager.getApplication().invokeLater {
                        if (timeToOptimizeImportsOnTheFly(file, editor, project) && editor.document.modificationStamp == modificationStamp) {
                            optimizeImportsOnTheFly(file, optimizedImports, editor, project)
                        }
                    }
                }
            }

            Disposer.register(progress, invokeFixLater)

            if (progress.isCanceled) {
                Disposer.dispose(invokeFixLater)
                Disposer.dispose(progress)
                progress.checkCanceled()
            }
        }
    }

    private fun timeToOptimizeImportsOnTheFly(file: KtFile, editor: Editor, project: Project): Boolean {
        if (project.isDisposed || !file.isValid || editor.isDisposed || !file.isWritable) return false

        // do not optimize imports on the fly during undo/redo
        val undoManager = UndoManager.getInstance(project)
        if (undoManager.isUndoInProgress || undoManager.isRedoInProgress) return false

        // if we stand inside import statements, do not optimize
        val importsRange = file.importList?.textRange ?: return false
        if (importsRange.containsOffset(editor.caretModel.offset)) return false

        val codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project)
        if (!codeAnalyzer.isHighlightingAvailable(file)) return false
        if (!codeAnalyzer.isErrorAnalyzingFinished(file)) return false

        val document = editor.document
        var hasErrors = false
        DaemonCodeAnalyzerEx.processHighlights(document, project, HighlightSeverity.ERROR, 0, document.textLength, Processor { highlightInfo ->
            if (!importsRange.containsRange(highlightInfo.startOffset, highlightInfo.endOffset)) {
                hasErrors = true
                false
            }
            else {
                true
            }
        })
        if (hasErrors) return false

        return DaemonListeners.canChangeFileSilently(file)
    }

    private fun optimizeImportsOnTheFly(file: KtFile, optimizedImports: List<ImportPath>, editor: Editor, project: Project) {
        PsiDocumentManager.getInstance(file.project).commitAllDocuments()
        DocumentUtil.writeInRunUndoTransparentAction {
            KotlinImportOptimizer.replaceImports(file, optimizedImports)
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        }
    }

    private class OptimizeImportsQuickFix(private val file: KtFile): LocalQuickFix {
        override fun getName() = "Optimize imports"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            OptimizeImportsProcessor(project, file).run()
        }
    }

    private class EnableOptimizeImportsOnTheFlyFix(private val file: KtFile) : LocalQuickFix, LowPriorityAction {
        override fun getName() = QuickFixBundle.message("enable.optimize.imports.on.the.fly")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY = true
            OptimizeImportsProcessor(project, file).run() // we optimize imports manually because on-the-fly import optimization won't work while the caret is in imports
        }
    }
}