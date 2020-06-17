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

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonListeners
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.*
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.DocumentUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.KotlinCodeInsightWorkspaceSettings
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.imports.KotlinImportOptimizer
import org.jetbrains.kotlin.idea.imports.OptimizedImportsBuilder
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.ImportPath

class KotlinUnusedImportInspection : AbstractKotlinInspection() {
    class ImportData(val unusedImports: List<KtImportDirective>, val optimizerData: OptimizedImportsBuilder.InputData)

    companion object {
        fun analyzeImports(file: KtFile): ImportData? {
            if (file is KtCodeFragment) return null
            if (!ProjectRootsUtil.isInProjectSource(file, true)) return null
            if (file.importDirectives.isEmpty()) return null

            val optimizerData = KotlinImportOptimizer.collectDescriptorsToImport(file)

            val directives = file.importDirectives
            val explicitlyImportedFqNames = directives
                .asSequence()
                .mapNotNull { it.importPath }
                .filter { !it.isAllUnder && !it.hasAlias() }
                .map { it.fqName }
                .toSet()

            val fqNames = optimizerData.namesToImport
            val parentFqNames = HashSet<FqName>()
            for (descriptor in optimizerData.descriptorsToImport) {
                val fqName = descriptor.importableFqName!!
                if (fqName !in explicitlyImportedFqNames) { // we don't add parents of explicitly imported fq-names because such imports are not needed
                    val parentFqName = fqName.parent()
                    if (!parentFqName.isRoot) {
                        parentFqNames.add(parentFqName)
                    }
                }
            }

            val invokeFunctionCallFqNames = optimizerData.references.mapNotNull {
                val reference = (it.element as? KtCallExpression)?.mainReference as? KtInvokeFunctionReference ?: return@mapNotNull null
                (reference.resolve() as? KtNamedFunction)?.descriptor?.importableFqName
            }

            val importPaths = HashSet<ImportPath>(directives.size)
            val unusedImports = ArrayList<KtImportDirective>()

            val resolutionFacade = file.getResolutionFacade()
            for (directive in directives) {
                val importPath = directive.importPath ?: continue

                val isUsed = when {
                    importPath.importedName in optimizerData.unresolvedNames &&
                            directive.targetDescriptors(resolutionFacade).isEmpty() -> true

                    !importPaths.add(importPath) -> false
                    importPath.isAllUnder -> optimizerData.unresolvedNames.isNotEmpty() || importPath.fqName in parentFqNames
                    importPath.fqName in fqNames -> importPath.importedName?.let { it in fqNames.getValue(importPath.fqName) } ?: false
                    importPath.fqName in invokeFunctionCallFqNames -> true
                    // case for type alias
                    else -> directive.targetDescriptors(resolutionFacade).firstOrNull()?.let { it.importableFqName in fqNames } ?: false
                }

                if (!isUsed) {
                    unusedImports += directive
                }
            }

            return ImportData(unusedImports, optimizerData)
        }
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (file !is KtFile) return null
        val data = analyzeImports(file) ?: return null

        val problems = data.unusedImports.map {
            val fixes = arrayListOf<LocalQuickFix>()
            fixes.add(OptimizeImportsQuickFix(file))
            if (!KotlinCodeInsightWorkspaceSettings.getInstance(file.project).optimizeImportsOnTheFly) {
                fixes.add(EnableOptimizeImportsOnTheFlyFix(file))
            }

            manager.createProblemDescriptor(
                it,
                KotlinBundle.message("unused.import.directive"),
                isOnTheFly,
                fixes.toTypedArray(),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
            )
        }

        if (isOnTheFly) {
            scheduleOptimizeImportsOnTheFly(file, data.optimizerData)
        }

        return problems.toTypedArray()
    }

    private fun scheduleOptimizeImportsOnTheFly(file: KtFile, data: OptimizedImportsBuilder.InputData) {
        if (!KotlinCodeInsightWorkspaceSettings.getInstance(file.project).optimizeImportsOnTheFly) return
        val optimizedImports = KotlinImportOptimizer.prepareOptimizedImports(file, data) ?: return // return if already optimized

        // unwrap progress indicator
        val progress = generateSequence(ProgressManager.getInstance().progressIndicator) {
            (it as? ProgressWrapper)?.originalProgressIndicator
        }.last() as DaemonProgressIndicator

        val project = file.project

        val modificationCount = PsiModificationTracker.SERVICE.getInstance(project).modificationCount
        val invokeFixLater = Disposable {
            // later because should invoke when highlighting is finished
            ApplicationManager.getApplication().invokeLater {
                val editor = PsiUtilBase.findEditor(file)
                val currentModificationCount = PsiModificationTracker.SERVICE.getInstance(project).modificationCount
                if (editor != null && currentModificationCount == modificationCount && timeToOptimizeImportsOnTheFly(
                        file,
                        editor,
                        project
                    )
                ) {
                    optimizeImportsOnTheFly(file, optimizedImports, editor, project)
                }
            }
        }

        if (Disposer.isDisposed(progress)) return
        Disposer.register(progress, invokeFixLater)

        if (progress.isCanceled) {
            Disposer.dispose(invokeFixLater)
            Disposer.dispose(progress)
            progress.checkCanceled()
        }
    }

    private fun timeToOptimizeImportsOnTheFly(file: KtFile, editor: Editor, project: Project): Boolean {
        if (project.isDisposed || !file.isValid || editor.isDisposed || !file.isWritable) return false

        // do not optimize imports on the fly during undo/redo
        val undoManager = UndoManager.getInstance(project)
        if (undoManager.isUndoInProgress || undoManager.isRedoInProgress) return false

        // if we stand inside import statements, do not optimize
        val importList = file.importList ?: return false
        val leftSpace = importList.siblings(forward = false, withItself = false).firstOrNull() as? PsiWhiteSpace
        val rightSpace = importList.siblings(forward = true, withItself = false).firstOrNull() as? PsiWhiteSpace
        val left = leftSpace ?: importList
        val right = rightSpace ?: importList
        val importsRange = TextRange(left.textRange.startOffset, right.textRange.endOffset)
        if (importsRange.containsOffset(editor.caretModel.offset)) return false

        val codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project)
        if (!codeAnalyzer.isHighlightingAvailable(file)) return false
        if (!codeAnalyzer.isErrorAnalyzingFinished(file)) return false

        val document = editor.document
        var hasErrors = false
        DaemonCodeAnalyzerEx.processHighlights(document, project, HighlightSeverity.ERROR, 0, document.textLength) { highlightInfo ->
            if (!importsRange.containsRange(highlightInfo.startOffset, highlightInfo.endOffset)) {
                hasErrors = true
                false
            } else {
                true
            }
        }
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

    private class OptimizeImportsQuickFix(file: KtFile) : LocalQuickFixOnPsiElement(file) {
        override fun getText() = KotlinBundle.message("optimize.imports")

        override fun getFamilyName() = name

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            OptimizeImportsProcessor(project, file).run()
        }
    }

    private class EnableOptimizeImportsOnTheFlyFix(file: KtFile) : LocalQuickFixOnPsiElement(file), LowPriorityAction {
        override fun getText(): String = QuickFixBundle.message("enable.optimize.imports.on.the.fly")

        override fun getFamilyName() = name

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            KotlinCodeInsightWorkspaceSettings.getInstance(project).optimizeImportsOnTheFly = true
            OptimizeImportsProcessor(
                project,
                file
            ).run() // we optimize imports manually because on-the-fly import optimization won't work while the caret is in imports
        }
    }
}