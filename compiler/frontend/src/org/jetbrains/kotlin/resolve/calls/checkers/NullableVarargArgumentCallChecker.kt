/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.BindingContext.EXPRESSION_TYPE_INFO
import org.jetbrains.kotlin.resolve.calls.components.stableType
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.SimplePSIKotlinCallArgument
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.TypeUtils

object NullableVarargArgumentCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return
        if (resolvedCall is VariableAsFunctionResolvedCall) {
            check(resolvedCall.functionCall, reportOn, context)
            return
        }
        if (resolvedCall !is NewResolvedCallImpl<*>) return
        for (argument in resolvedCall.argumentMappingByOriginal.values) {
            for (arg in argument.arguments) {
                if (!arg.isSpread || arg !is SimplePSIKotlinCallArgument) continue

                val spreadElement = arg.valueArgument.getSpreadElement() ?: continue
                val receiver = arg.receiver

                val type = if (receiver.stableType.constructor is TypeVariableTypeConstructor) {
                    context.trace.bindingContext[EXPRESSION_TYPE_INFO, arg.valueArgument.getArgumentExpression()]?.type
                        ?: receiver.stableType
                } else {
                    receiver.stableType
                }

                if (type !is FlexibleType && TypeUtils.isNullableType(type)) {
                    context.trace.report(Errors.SPREAD_OF_NULLABLE.on(spreadElement))
                }
            }
        }
    }
}
