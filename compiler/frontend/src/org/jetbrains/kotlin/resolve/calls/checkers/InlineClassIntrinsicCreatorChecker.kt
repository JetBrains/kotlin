/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.inlineClassRepresentation
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

object InlineClassIntrinsicCreatorChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.CustomBoxingInInlineClasses)) return
        val callExpression = reportOn.getStrictParentOfType<KtCallExpression>() ?: return
        val calleeDescriptor = resolvedCall.resultingDescriptor
        if (calleeDescriptor.name != StandardNames.INTRINSIC_DEFAULT_BOXING_NAME || calleeDescriptor.containingPackage() != StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
            return
        }
        if (resolvedCall.typeArguments.size != 1) return
        val typeArgument = resolvedCall.typeArguments.values.singleOrNull() ?: return
        if (typeArgument.arguments.isNotEmpty() || !typeArgument.isInlineClassType()) {
            if (callExpression.typeArguments.size != 1) {
                context.trace.report(Errors.INTRINSIC_BOXING_CALL_BAD_INFERRED_TYPE_ARGUMENT.on(callExpression))
            } else {
                context.trace.report(Errors.INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT.on(callExpression.typeArguments[0]))
            }
            return
        }
        val valueArgument = resolvedCall.valueArgumentsByIndex?.getOrNull(0)?.arguments?.getOrNull(0)?.getArgumentExpression() ?: return
        val valueArgumentType = valueArgument.getType(context.trace.bindingContext) ?: return
        val underlyingType =
            (typeArgument.constructor.declarationDescriptor as? ClassDescriptor)?.inlineClassRepresentation?.underlyingType ?: return
        if (!valueArgumentType.isSubtypeOf(underlyingType)) {
            context.trace.report(
                Errors.INTRINSIC_BOXING_CALL_ARGUMENT_TYPE_MISMATCH.on(
                    valueArgument,
                    valueArgumentType,
                    typeArgument,
                    underlyingType
                )
            )
        }
    }
}