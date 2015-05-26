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

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.highlighter.JetPsiChecker
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.ReplaceObsoleteLabelSyntaxFix
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

public class KotlinCleanupInspection(): LocalInspectionTool(), CleanupLocalInspectionTool {
    // required to simplify the inspection registration in tests
    override fun getDisplayName(): String = "Usage of redundant or deprecated syntax"

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
        file.acceptChildren(object: JetTreeVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                diagnostics.forElement(element)
                   .filter { it.isCleanup() }
                   .map { it.toProblemDescriptor(file, manager) }
                   .filterNotNullTo(problemDescriptors)
            }
        })
        return problemDescriptors.toTypedArray()
    }

    private fun Diagnostic.isCleanup() = getFactory() in cleanupDiagnosticsFactories || isObsoleteLabel()

    private val cleanupDiagnosticsFactories = setOf(
            Errors.DEPRECATED_TRAIT_KEYWORD,
            Errors.DEPRECATED_ANNOTATION_SYNTAX,
            Errors.ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER,
            Errors.ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR,
            Errors.DEPRECATED_LAMBDA_SYNTAX,
            Errors.MISSING_CONSTRUCTOR_KEYWORD,
            Errors.FUNCTION_EXPRESSION_WITH_NAME,
            Errors.JAVA_LANG_CLASS_PARAMETER_IN_ANNOTATION,
            ErrorsJvm.JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION,
            Errors.UNNECESSARY_NOT_NULL_ASSERTION,
            Errors.UNNECESSARY_SAFE_CALL,
            Errors.USELESS_CAST,
            Errors.USELESS_ELVIS
    )

    private fun Diagnostic.isObsoleteLabel(): Boolean {
        val annotationEntry = getPsiElement().getNonStrictParentOfType<JetAnnotationEntry>() ?: return false
        return ReplaceObsoleteLabelSyntaxFix.looksLikeObsoleteLabel(annotationEntry)
    }

    private fun Diagnostic.toProblemDescriptor(file: JetFile, manager: InspectionManager): ProblemDescriptor? {
        val quickFixes = JetPsiChecker.createQuickfixes(this)
                .filter { it is CleanupFix }
                .map { IntentionWrapper(it, file) }

        if (quickFixes.isEmpty()) return null

        return manager.createProblemDescriptor(getPsiElement(),
                                                DefaultErrorMessages.render(this),
                                                false,
                                                quickFixes.toTypedArray(),
                                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }
}
