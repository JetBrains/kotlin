/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiChecker
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.ReplaceObsoleteLabelSyntaxFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class KotlinCleanupInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    // required to simplify the inspection registration in tests
    override fun getDisplayName(): String = KotlinBundle.message("usage.of.redundant.or.deprecated.syntax.or.deprecated.symbols")

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (isOnTheFly || file !is KtFile || !ProjectRootsUtil.isInProjectSource(file)) {
            return null
        }

        val analysisResult = file.analyzeWithAllCompilerChecks()
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }

        val diagnostics = analysisResult.bindingContext.diagnostics

        val problemDescriptors = arrayListOf<ProblemDescriptor>()

        val importsToRemove = DeprecatedSymbolUsageFix.importDirectivesToBeRemoved(file)
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

    private fun Diagnostic.isCleanup() = factory in cleanupDiagnosticsFactories || isObsoleteLabel()

    private val cleanupDiagnosticsFactories = setOf(
        Errors.MISSING_CONSTRUCTOR_KEYWORD,
        Errors.UNNECESSARY_NOT_NULL_ASSERTION,
        Errors.UNNECESSARY_SAFE_CALL,
        Errors.USELESS_CAST,
        Errors.USELESS_ELVIS,
        ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION,
        Errors.DEPRECATION,
        Errors.DEPRECATION_ERROR,
        Errors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION,
        Errors.OPERATOR_MODIFIER_REQUIRED,
        Errors.INFIX_MODIFIER_REQUIRED,
        Errors.DEPRECATED_TYPE_PARAMETER_SYNTAX,
        Errors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS,
        Errors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT,
        ErrorsJs.WRONG_EXTERNAL_DECLARATION,
        Errors.YIELD_IS_RESERVED,
        Errors.DEPRECATED_MODIFIER_FOR_TARGET,
        Errors.DEPRECATED_MODIFIER
    )

    private fun Diagnostic.isObsoleteLabel(): Boolean {
        val annotationEntry = psiElement.getNonStrictParentOfType<KtAnnotationEntry>() ?: return false
        return ReplaceObsoleteLabelSyntaxFix.looksLikeObsoleteLabel(annotationEntry)
    }

    private fun Diagnostic.toCleanupFixes(): Collection<CleanupFix> {
        return KotlinPsiChecker.createQuickFixes(this).filterIsInstance<CleanupFix>()
    }

    private class Wrapper(val intention: IntentionAction, file: KtFile) : IntentionWrapper(intention, file) {
        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            if (intention.isAvailable(
                    project,
                    editor,
                    file
                )
            ) { // we should check isAvailable here because some elements may get invalidated (or other conditions may change)
                super.invoke(project, editor, file)
            }
        }
    }

    private fun Diagnostic.toProblemDescriptor(fixes: Collection<CleanupFix>, file: KtFile, manager: InspectionManager): ProblemDescriptor =
        createProblemDescriptor(psiElement, DefaultErrorMessages.render(this), fixes, file, manager)

    private fun createProblemDescriptor(
        element: PsiElement,
        message: String,
        fixes: Collection<CleanupFix>,
        file: KtFile,
        manager: InspectionManager
    ): ProblemDescriptor {
        return manager.createProblemDescriptor(
            element,
            message,
            false,
            fixes.map { Wrapper(it, file) }.toTypedArray(),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )
    }

    private class RemoveImportFix(import: KtImportDirective) : KotlinQuickFixAction<KtImportDirective>(import), CleanupFix {
        override fun getFamilyName() = KotlinBundle.message("remove.deprecated.symbol.import")
        override fun getText() = familyName

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            element?.delete()
        }
    }
}
