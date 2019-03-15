/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

class DirectUseOfResultTypeInspection : AbstractIsResultInspection(
    typeShortName = SHORT_NAME,
    typeFullName = "kotlin.Result",
    allowedSuffix = CATCHING,
    allowedNames = setOf("success", "failure", "runCatching"),
    suggestedFunctionNameToCall = "getOrThrow"
) {

    private fun MemberScope.hasCorrespondingNonCatchingFunction(
        nameWithoutCatching: String,
        valueParameters: List<ValueParameterDescriptor>,
        returnType: KotlinType?,
        extensionReceiverType: KotlinType?
    ): Boolean {
        val nonCatchingFunctions = getContributedFunctions(Name.identifier(nameWithoutCatching), NoLookupLocation.FROM_IDE)
        return nonCatchingFunctions.any { nonCatchingFun ->
            nonCatchingFun.valueParameters.size == valueParameters.size
                    && nonCatchingFun.returnType == returnType
                    && nonCatchingFun.extensionReceiverParameter?.type == extensionReceiverType
        }
    }

    private fun FunctionDescriptor.hasCorrespondingNonCatchingFunction(returnType: KotlinType, nameWithoutCatching: String): Boolean? {
        val containingDescriptor = containingDeclaration
        val scope = when (containingDescriptor) {
            is ClassDescriptor -> containingDescriptor.unsubstitutedMemberScope
            is PackageFragmentDescriptor -> containingDescriptor.getMemberScope()
            else -> return null
        }
        val returnTypeArgument = returnType.arguments.firstOrNull()?.type
        val extensionReceiverType = extensionReceiverParameter?.type
        if (scope.hasCorrespondingNonCatchingFunction(nameWithoutCatching, valueParameters, returnTypeArgument, extensionReceiverType)) {
            return true
        }
        if (extensionReceiverType != null) {
            val extensionClassDescriptor = extensionReceiverType.constructor.declarationDescriptor as? ClassDescriptor
            if (extensionClassDescriptor != null) {
                val extensionClassScope = extensionClassDescriptor.unsubstitutedMemberScope
                if (extensionClassScope.hasCorrespondingNonCatchingFunction(
                        nameWithoutCatching, valueParameters, returnTypeArgument, extensionReceiverType = null
                    )
                ) {
                    return true
                }
            }
        }
        return false
    }

    override fun analyzeFunctionWithAllowedSuffix(
        name: String,
        descriptor: FunctionDescriptor,
        toReport: PsiElement,
        holder: ProblemsHolder
    ) {
        val returnType = descriptor.returnType ?: return
        val nameWithoutCatching = name.substringBeforeLast(CATCHING)
        if (descriptor.hasCorrespondingNonCatchingFunction(returnType, nameWithoutCatching) == false) {
            val returnTypeArgument = returnType.arguments.firstOrNull()?.type
            val typeName = returnTypeArgument?.constructor?.declarationDescriptor?.name?.asString() ?: "T"
            holder.registerProblem(
                toReport,
                "Function '$name' returning '$SHORT_NAME<$typeName>' without the corresponding " +
                        "function '$nameWithoutCatching' returning '$typeName'",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }
}

private const val SHORT_NAME = "Result"

private const val CATCHING = "Catching"
