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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class SimplifyCallChainFix(val newName: String) : LocalQuickFix {
    override fun getName() = "Merge call chain to '$newName'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtQualifiedExpression)?.let { secondQualifiedExpression ->
            val factory = KtPsiFactory(secondQualifiedExpression)
            val firstQualifiedExpression = secondQualifiedExpression.receiverExpression as? KtQualifiedExpression ?: return
            val operationSign = if (firstQualifiedExpression is KtSafeQualifiedExpression) "?." else "."
            val firstCallExpression = firstQualifiedExpression.selectorExpression as? KtCallExpression ?: return
            val secondCallExpression = secondQualifiedExpression.selectorExpression as? KtCallExpression ?: return

            val lastArgumentPrefix = if (newName.startsWith("joinTo")) "transform = " else ""
            val arguments = secondCallExpression.valueArgumentList?.arguments.orEmpty().map { it.text } +
                            firstCallExpression.valueArgumentList?.arguments.orEmpty().map { "$lastArgumentPrefix${it.text}"}
            val lambdaArgument = firstCallExpression.lambdaArguments.singleOrNull()

            val argumentsText = arguments.ifNotEmpty { joinToString(prefix = "(", postfix = ")") } ?: ""
            val newQualifiedExpression = if (lambdaArgument != null) factory.createExpressionByPattern(
                    "$0$1$2 $3 $4",
                    firstQualifiedExpression.receiverExpression,
                    operationSign,
                    newName,
                    argumentsText,
                    lambdaArgument.getLambdaExpression().text
            )
            else factory.createExpressionByPattern(
                    "$0$1$2 $3",
                    firstQualifiedExpression.receiverExpression,
                    operationSign,
                    newName,
                    argumentsText
            )

            secondQualifiedExpression.replaced(newQualifiedExpression)
        }
    }
}