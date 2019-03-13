/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

class SimplifyCallChainFix(
    private val conversion: AbstractCallChainChecker.Conversion,
    private val removeReceiverOfFirstCall: Boolean = false,
    private val runOptimizeImports: Boolean = false,
    private val modifyArguments: KtPsiFactory.(KtCallExpression) -> Unit = {}
) : LocalQuickFix {
    private val shortenedText = conversion.replacement.substringAfterLast(".")

    override fun getName() = "Merge call chain to '$shortenedText'"

    override fun getFamilyName() = name

    fun apply(qualifiedExpression: KtQualifiedExpression) {
        val commentSaver = CommentSaver(qualifiedExpression)
        val factory = KtPsiFactory(qualifiedExpression)
        val firstExpression = qualifiedExpression.receiverExpression

        val operationSign = if (removeReceiverOfFirstCall) "" else when (firstExpression) {
            is KtSafeQualifiedExpression -> "?."
            is KtQualifiedExpression -> "."
            else -> ""
        }

        val receiverExpressionOrEmptyString =
            if (!removeReceiverOfFirstCall && firstExpression is KtQualifiedExpression) firstExpression.receiverExpression.text else ""

        val firstCallExpression = AbstractCallChainChecker.getCallExpression(firstExpression) ?: return
        factory.modifyArguments(firstCallExpression)
        val firstCallArgumentList = firstCallExpression.valueArgumentList

        val secondCallExpression = qualifiedExpression.selectorExpression as? KtCallExpression ?: return
        val secondCallArgumentList = secondCallExpression.valueArgumentList

        fun KtValueArgumentList.getTextInsideParentheses(): String {
            val range = PsiChildRange(leftParenthesis?.nextSibling ?: firstChild, rightParenthesis?.prevSibling ?: lastChild)
            return range.joinToString(separator = "") { it.text }
        }

        val lambdaExpression = firstCallExpression.lambdaArguments.singleOrNull()?.getLambdaExpression()
        val additionalArgument = conversion.additionalArgument
        val secondCallHasArguments = secondCallArgumentList?.arguments?.isNotEmpty() == true
        val firstCallHasArguments = firstCallArgumentList?.arguments?.isNotEmpty() == true
        val argumentsText = listOfNotNull(
            secondCallArgumentList.takeIf { secondCallHasArguments }?.getTextInsideParentheses(),
            firstCallArgumentList.takeIf { firstCallHasArguments }?.getTextInsideParentheses(),
            additionalArgument.takeIf { !firstCallHasArguments && !secondCallHasArguments },
            lambdaExpression?.text
        ).joinToString(separator = ",")

        val newCallText = conversion.replacement
        val newQualifiedOrCallExpression = factory.createExpression(
            "$receiverExpressionOrEmptyString$operationSign$newCallText($argumentsText)"
        )

        val project = qualifiedExpression.project
        val file = qualifiedExpression.containingKtFile
        val result = qualifiedExpression.replaced(newQualifiedOrCallExpression)

        if (!firstCallHasArguments && !secondCallHasArguments) {
            commentSaver.restore(result)
        }
        if (lambdaExpression != null) {
            val callExpression = when (result) {
                is KtQualifiedExpression -> result.callExpression
                is KtCallExpression -> result
                else -> null
            }
            callExpression?.moveFunctionLiteralOutsideParentheses()
        }

        result.containingKtFile.commitAndUnblockDocument()
        val reformatted = CodeStyleManager.getInstance(project).reformat(result)
        ShortenReferences.DEFAULT.process(reformatted as KtElement)
        if (runOptimizeImports) {
            OptimizeImportsProcessor(project, file).run()
        }
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtQualifiedExpression)?.let(this::apply)
    }
}