/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.isSynthesized
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.IndexedParametersSubstitution
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.BasicExpressionTypingVisitor
import org.jetbrains.kotlin.types.typeUtil.isNothing

object JvmSyntheticAssignmentChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (!resultingDescriptor.isSynthesized) return
        if (resultingDescriptor !is SyntheticJavaPropertyDescriptor) return
        if (reportOn !is KtNameReferenceExpression) return
        val binaryExpression = reportOn.getParentOfType<KtBinaryExpression>(strict = true) ?: return
        if (!BasicExpressionTypingVisitor.isLValue(reportOn, binaryExpression)) return
        val receiverType = resolvedCall.extensionReceiver?.type ?: return
        val unsubstitutedReceiverType = resolvedCall.candidateDescriptor.extensionReceiverParameter?.type ?: return
        if (receiverType.constructor !== unsubstitutedReceiverType.constructor) return
        val propertyType = resolvedCall.candidateDescriptor.returnType ?: return

        val substitutionParameters = mutableListOf<TypeParameterDescriptor>()
        val substitutionArguments = mutableListOf<TypeProjection>()
        for ((unsubstitutedArgument, substitutedArgument) in unsubstitutedReceiverType.arguments.zip(receiverType.arguments)) {
            val typeParameter = unsubstitutedArgument.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: continue
            substitutionParameters += typeParameter
            substitutionArguments += substitutedArgument
        }
        val substitutor = TypeSubstitutor.create(
            IndexedParametersSubstitution(
                substitutionParameters.toTypedArray(), substitutionArguments.toTypedArray(), approximateContravariantCapturedTypes = true
            )
        )
        val substitutedPropertyType = substitutor.substitute(propertyType.unwrap(), Variance.IN_VARIANCE)
        if (substitutedPropertyType == null || !substitutedPropertyType.isNothing()) return
        context.trace.report(ErrorsJvm.SYNTHETIC_SETTER_PROJECTED_OUT.on(binaryExpression.left ?: reportOn, resultingDescriptor))
    }
}