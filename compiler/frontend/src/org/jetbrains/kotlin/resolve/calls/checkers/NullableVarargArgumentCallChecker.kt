/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.components.stableType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.ExpressionKotlinCallArgumentImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewVariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
                if (!arg.isSpread || arg !is ExpressionKotlinCallArgumentImpl) continue
                val spreadElement = arg.valueArgument.getSpreadElement() ?: continue

                val receiver = arg.receiver.safeAs<ReceiverValueWithSmartCastInfo>() ?: continue
                val type = receiver.stableType
                if (type !is FlexibleType && TypeUtils.isNullableType(type)) {
                    context.trace.report(Errors.SPREAD_OF_NULLABLE.on(spreadElement))
                }
            }
        }
    }
}