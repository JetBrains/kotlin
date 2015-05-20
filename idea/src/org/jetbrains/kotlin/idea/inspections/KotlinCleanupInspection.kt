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
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.highlighter.JetPsiChecker
import org.jetbrains.kotlin.idea.quickfix.JetWholeProjectModalAction
import org.jetbrains.kotlin.idea.quickfix.ReplaceObsoleteLabelSyntaxFix
import org.jetbrains.kotlin.idea.quickfix.looksLikeObsoleteLabel
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

public class KotlinCleanupInspection(): LocalInspectionTool(), CleanupLocalInspectionTool {
    // required to simplify the inspection registration in tests
    override fun getDisplayName(): String = "Deprecated language feature"

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
                val collection = diagnostics.forElement(element)
                collection.forEach {
                    if (it.isCleanup()) {
                        problemDescriptors.add(it.toProblemDescriptor(file, manager))
                    }
                }
            }
        })
        return problemDescriptors.toTypedArray()
    }

    private fun Diagnostic.isCleanup() = getFactory().isCleanup() || isObsoleteLabel()

    private fun DiagnosticFactory<*>.isCleanup() =
            this == Errors.DEPRECATED_TRAIT_KEYWORD ||
            this == Errors.DEPRECATED_ANNOTATION_SYNTAX ||
            this == Errors.ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER ||
            this == Errors.ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR ||
            this == Errors.DEPRECATED_LAMBDA_SYNTAX ||
            this == Errors.MISSING_CONSTRUCTOR_KEYWORD ||
            this == Errors.FUNCTION_EXPRESSION_WITH_NAME ||
            this == Errors.JAVA_LANG_CLASS_PARAMETER_IN_ANNOTATION ||
            this == ErrorsJvm.JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION

    private fun Diagnostic.isObsoleteLabel(): Boolean {
        val annotationEntry = getPsiElement().getNonStrictParentOfType<JetAnnotationEntry>() ?: return false
        return annotationEntry.looksLikeObsoleteLabel()
    }

    private fun Diagnostic.toProblemDescriptor(file: JetFile, manager: InspectionManager): ProblemDescriptor? {
        val quickFixes = JetPsiChecker.createQuickfixes(this)
                .filter { it.isCleanupFix(this) }
                .map { IntentionWrapper(it, file) }

         return manager.createProblemDescriptor(getPsiElement(),
                                                DefaultErrorMessages.render(this),
                                                false,
                                                quickFixes.toTypedArray(),
                                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }

    private fun IntentionAction.isCleanupFix(diagnostic: Diagnostic): Boolean {
        if (diagnostic.getFactory() == Errors.UNRESOLVED_REFERENCE) {
            return this is ReplaceObsoleteLabelSyntaxFix
        }
        return this !is JetWholeProjectModalAction<*>
    }
}
