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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class JavaCollectionsStaticMethodInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                val callExpression = expression.callExpression ?: return
                val context = callExpression.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = callExpression.getResolvedCall(context) ?: return
                val descriptor = resolvedCall.resultingDescriptor as? JavaMethodDescriptor ?: return
                val fqName = descriptor.importableFqName?.asString() ?: return
                if (!canReplaceWithStdLib(fqName, callExpression, context)) return

                holder.registerProblem(expression,
                                       "Java Collections static method call can be replaced with Kotlin stdlib",
                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                       ReplaceWithSortFix(fqName))
            }
        }
    }

    private fun canReplaceWithStdLib(fqName: String, callExpression: KtCallExpression, context: BindingContext): Boolean {
        if (!fqName.startsWith("java.util.Collections.")) return false
        val valueArgs = callExpression.valueArguments
        val valueArgsSize = valueArgs.size
        return when (fqName) {
            "java.util.Collections.reverse" ->
                valueArgsSize == 1 && isMutableList(valueArgs[0], context)
            "java.util.Collections.sort" ->
                (valueArgsSize == 1 || valueArgsSize == 2) && isMutableList(valueArgs[0], context)
            else ->
                false
        }
    }

    private fun isMutableList(arg: KtValueArgument?, context: BindingContext): Boolean {
        val expression = arg?.getArgumentExpression() ?: return false
        return expression.getType(context)?.nameIfStandardType?.asString() == "MutableList"
    }
}

private class ReplaceWithSortFix(private val fqName: String) : LocalQuickFix {
    override fun getName() = "Replace with stdlib"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtDotQualifiedExpression ?: return
        val callExpression = expression.callExpression ?: return
        val valueArguments = callExpression.valueArguments
        val arg1 = valueArguments.getOrNull(0)?.getArgumentExpression()
        val arg2 = valueArguments.getOrNull(1)?.getArgumentExpression()
        val factory = KtPsiFactory(project)
        val newExpression = when (fqName) {
            "java.util.Collections.reverse" -> when {
                arg1 != null -> factory.createExpressionByPattern("$0.reverse()", arg1)
                else -> null
            }
            "java.util.Collections.sort" -> when {
                arg1 != null && arg2 != null -> factory.createExpressionByPattern("$0.sortWith(Comparator $1)", arg1, arg2.text)
                arg1 != null -> factory.createExpressionByPattern("$0.sort()", arg1)
                else -> null
            }
            else -> null
        }
        if (newExpression != null) expression.replace(newExpression)
    }
}
