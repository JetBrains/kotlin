/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class JavaCollectionsStaticMethodInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression) {
            val callExpression = expression.callExpression ?: return
            val args = callExpression.valueArguments
            val firstArg = args.firstOrNull() ?: return
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            if (!firstArg.isMutableList(context)) return

            val descriptor = expression.getResolvedCall(context)?.resultingDescriptor as? JavaMethodDescriptor ?: return
            val fqName = descriptor.importableFqName?.asString() ?: return
            if (!canReplaceWithStdLib(expression, fqName, args)) return

            val methodName = fqName.split(".").last()
            holder.registerProblem(
                expression,
                "Java Collections static method call should be replaced with Kotlin stdlib",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceWithStdLibFix(methodName, firstArg.text)
            )
        })
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

private fun KtValueArgument.isMutableList(context: BindingContext): Boolean {
    val type = getArgumentExpression()?.getType(context) ?: return false
    val constructor = type.constructor
    val mutableListType = type.builtIns.mutableList.defaultType
    if (constructor.declarationDescriptor?.defaultType?.isSubtypeOf(mutableListType) == true) return true
    return constructor.supertypes.reversed().any {
        it.constructor.declarationDescriptor?.defaultType?.isSubtypeOf(mutableListType) == true
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
            if (methodName == "sort") {
                factory.createExpressionByPattern("$0.sortWith(Comparator $1)", firstArg, secondArg.text)
            } else {
                factory.createExpressionByPattern("$0.$methodName($1)", firstArg, secondArg)
            }
        } else {
            factory.createExpressionByPattern("$0.$methodName()", firstArg)
        }
        expression.replace(newExpression)
    }
}
