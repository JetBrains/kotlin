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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.highlighter.JetPsiChecker
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.ReplaceObsoleteLabelSyntaxFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

public class KotlinCleanupInspection(): LocalInspectionTool(), CleanupLocalInspectionTool {
    // required to simplify the inspection registration in tests
    override fun getDisplayName(): String = "Usage of redundant or deprecated syntax or deprecated symbols"

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (isOnTheFly || file !is JetFile || !ProjectRootsUtil.isInProjectSource(file)) {
            return null
        }

        val analysisResult = file.analyzeFullyAndGetResult()
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }

        val diagnostics = analysisResult.bindingContext.getDiagnostics()

        val problemDescriptors = arrayListOf<ProblemDescriptor>()

        val importsToRemove = file.importDirectives.filter { DeprecatedSymbolUsageFix.isImportToBeRemoved(it) }
        for (import in importsToRemove) {
            val removeImportFix = RemoveImportFix(import)
            val problemDescriptor = createProblemDescriptor(import, removeImportFix.text, listOf(removeImportFix), file, manager)
            problemDescriptors.add(problemDescriptor)
        }

        file.forEachDescendantOfType<PsiElement> { element ->
            for (diagnostic in diagnostics.forElement(element)) {
                if (diagnostic.isCleanup()) {
                    val fixes = diagnostic.toCleanupFixes()
                    if (fixes.isNotEmpty()) {
                        problemDescriptors.add(diagnostic.toProblemDescriptor(fixes, file, manager))
                    }
                }
            }
        }

        return problemDescriptors.toTypedArray()
    }

    private fun Diagnostic.isCleanup() = getFactory() in cleanupDiagnosticsFactories || isObsoleteLabel()

    private val cleanupDiagnosticsFactories = setOf(
            Errors.MISSING_CONSTRUCTOR_KEYWORD,
            Errors.UNNECESSARY_NOT_NULL_ASSERTION,
            Errors.UNNECESSARY_SAFE_CALL,
            Errors.USELESS_CAST,
            Errors.USELESS_ELVIS,
            ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION,
            Errors.DEPRECATED_SYMBOL_WITH_MESSAGE,
            Errors.ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE
    )

    private fun Diagnostic.isObsoleteLabel(): Boolean {
        val annotationEntry = getPsiElement().getNonStrictParentOfType<JetAnnotationEntry>() ?: return false
        return ReplaceObsoleteLabelSyntaxFix.looksLikeObsoleteLabel(annotationEntry)
    }

    private fun Diagnostic.toCleanupFixes(): Collection<CleanupFix> {
        return JetPsiChecker.createQuickfixes(this).filterIsInstance<CleanupFix>()
    }

    private class Wrapper(val intention: IntentionAction, file: JetFile) : IntentionWrapper(intention, file) {
        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            if (intention.isAvailable(project, editor, file)) { // we should check isAvailable here because some elements may get invalidated (or other conditions may change)
                super.invoke(project, editor, file)
            }
        }
    }

    private fun Diagnostic.toProblemDescriptor(fixes: Collection<CleanupFix>, file: JetFile, manager: InspectionManager): ProblemDescriptor {
        return createProblemDescriptor(psiElement, DefaultErrorMessages.render(this), fixes, file, manager)
    }

    private fun createProblemDescriptor(element: PsiElement, message: String, fixes: Collection<CleanupFix>, file: JetFile, manager: InspectionManager): ProblemDescriptor {
        return manager.createProblemDescriptor(element,
                                               message,
                                               false,
                                               fixes.map { Wrapper(it, file) }.toTypedArray(),
                                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }

    private class RemoveImportFix(import: JetImportDirective) : JetIntentionAction<JetImportDirective>(import), CleanupFix {
        override fun getFamilyName() = "Remove deprecated symbol import"
        override fun getText() = familyName

        override fun invoke(project: Project, editor: Editor?, file: JetFile) {
            element.delete()
        }
    }
}
