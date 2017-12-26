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
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class JavaCollectionsStaticMethodInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                val callExpression = expression.callExpression ?: return
                val args = callExpression.valueArguments
                val firstArg = args.firstOrNull() ?: return
                val context = expression.analyze(BodyResolveMode.PARTIAL)
                if (KotlinBuiltIns.FQ_NAMES.mutableList !=
                        firstArg.getArgumentExpression()?.getType(context)?.constructor?.declarationDescriptor?.fqNameSafe) return

                val resolvedCall = expression.getResolvedCall(context) ?: return
                val descriptor = resolvedCall.resultingDescriptor as? JavaMethodDescriptor ?: return
                val fqName = descriptor.importableFqName?.asString() ?: return
                if (!canReplaceWithStdLib(expression, fqName, args)) return

                val methodName = fqName.split(".").last()
                holder.registerProblem(expression,
                                       "Java Collections static method call should be replaced with Kotlin stdlib",
                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                       ReplaceWithStdLibFix(methodName, firstArg.text))
            }
        }
    }

    private fun canReplaceWithStdLib(expression: KtDotQualifiedExpression, fqName: String, args: List<KtValueArgument>): Boolean {
        if (!fqName.startsWith("java.util.Collections.")) return false
        val size = args.size
        return when (fqName) {
            "java.util.Collections.fill" -> checkApiVersion(ApiVersion.KOTLIN_1_2, expression) && size == 2
            "java.util.Collections.reverse" -> size == 1
            "java.util.Collections.shuffle" -> checkApiVersion(ApiVersion.KOTLIN_1_2, expression) && (size == 1 || size == 2)
            "java.util.Collections.sort" -> {
                size == 1 || (size == 2 && args.getOrNull(1)?.getArgumentExpression() is KtLambdaExpression)
            }
            else -> false
        }
    }

    private fun checkApiVersion(requiredVersion: ApiVersion, expression: KtDotQualifiedExpression): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(expression) ?: return true
        return module.languageVersionSettings.apiVersion >= requiredVersion
    }

}

private class ReplaceWithStdLibFix(private val methodName: String, private val receiver: String) : LocalQuickFix {
    override fun getName() = "Replace with $receiver.$methodName"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtDotQualifiedExpression ?: return
        val callExpression = expression.callExpression ?: return
        val valueArguments = callExpression.valueArguments
        val firstArg = valueArguments.getOrNull(0)?.getArgumentExpression() ?: return
        val secondArg = valueArguments.getOrNull(1)?.getArgumentExpression()
        val factory = KtPsiFactory(project)
        val newExpression = if (secondArg != null) {
            if (methodName == "sort")
                factory.createExpressionByPattern("$0.sortWith(Comparator $1)", firstArg, secondArg.text)
            else
                factory.createExpressionByPattern("$0.$methodName($1)", firstArg, secondArg)
        }
        else
            factory.createExpressionByPattern("$0.$methodName()", firstArg)
        expression.replace(newExpression)
    }
}
