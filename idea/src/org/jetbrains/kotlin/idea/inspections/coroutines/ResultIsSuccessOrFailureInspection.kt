/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ResultIsSuccessOrFailureInspection : AbstractKotlinInspection() {
    private fun analyzeFunction(function: KtFunction, toReport: PsiElement, holder: ProblemsHolder) {
        if (function is KtConstructor<*>) return
        val returnTypeText = function.getReturnTypeReference()?.text
        if (returnTypeText != null && SHORT_NAME !in returnTypeText) return

        val descriptor = function.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return
        val returnType = descriptor.returnType ?: return
        val returnTypeClass = returnType.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (returnTypeClass.fqNameSafe.asString() != FULL_NAME) return

        val name = (function as? KtNamedFunction)?.nameAsName?.asString()
        if (name != null && name.endsWith(CATCHING)) {
            val nameWithoutCatching = name.substringBeforeLast(CATCHING)
            val containingDescriptor = descriptor.containingDeclaration
            val scope = when (containingDescriptor) {
                is ClassDescriptor -> containingDescriptor.unsubstitutedMemberScope
                is PackageFragmentDescriptor -> containingDescriptor.getMemberScope()
                else -> return
            }
            val returnTypeArgument = returnType.arguments.firstOrNull()?.type
            val nonCatchingFunctions = scope.getContributedFunctions(Name.identifier(nameWithoutCatching), NoLookupLocation.FROM_IDE)
            if (nonCatchingFunctions.none { nonCatchingFun ->
                    nonCatchingFun.returnType == returnTypeArgument
                }) {
                val typeName = returnTypeArgument?.constructor?.declarationDescriptor?.name?.asString() ?: "T"
                holder.registerProblem(
                    toReport,
                    "Function '$name' returning '$SHORT_NAME<$typeName>' without the corresponding " +
                            "function '$nameWithoutCatching' returning '$typeName'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        } else {
            holder.registerProblem(
                toReport,
                "Function returning $SHORT_NAME with a name that does not end with $CATCHING",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                analyzeFunction(function, function.nameIdentifier ?: function.funKeyword ?: function, holder)
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                analyzeFunction(lambdaExpression.functionLiteral, lambdaExpression.functionLiteral.lBrace, holder)
            }
        }
    }

    companion object {
        private const val SHORT_NAME = "SuccessOrFailure"

        private const val FULL_NAME = "kotlin.$SHORT_NAME"

        private const val CATCHING = "Catching"
    }
}