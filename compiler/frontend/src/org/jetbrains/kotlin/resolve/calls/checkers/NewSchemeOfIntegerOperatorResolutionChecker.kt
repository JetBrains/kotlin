/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.lowerIfFlexible

object NewSchemeOfIntegerOperatorResolutionChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ApproximateIntegerLiteralTypesInReceiverPosition)) return
        val bindingContext = context.trace.bindingContext
        for ((valueParameter, arguments) in resolvedCall.valueArguments) {
            val expectedType = if (valueParameter.isVararg) {
                valueParameter.varargElementType ?: continue
            } else {
                valueParameter.type
            }.unwrap().lowerIfFlexible()
            for (argument in arguments.arguments) {
                val expression = argument.getArgumentExpression() ?: continue
                val compileTimeValue =
                    bindingContext[BindingContext.COMPILE_TIME_VALUE, expression] as? IntegerValueTypeConstant? ?: continue
                val callForArgument = expression.getResolvedCall(bindingContext) ?: continue
                if (!callForArgument.isIntOperator()) continue
                val callElement = callForArgument.call.callElement
                if (callElement is KtConstantExpression) continue
                if (callElement is KtUnaryExpression) {
                    val token = callElement.operationToken
                    if (token == KtTokens.PLUS || token == KtTokens.MINUS) continue
                }

                val valueTypeConstructor = compileTimeValue.unknownIntegerType.constructor as? IntegerLiteralTypeConstructor ?: continue
                val approximatedType = valueTypeConstructor.getApproximatedType()
                if (approximatedType != expectedType) {
                    context.trace.report(Errors.INTEGER_OPERATOR_RESOLVE_WILL_CHANGE.on(expression, approximatedType))
                }
            }
        }
    }

    private fun ResolvedCall<*>.isIntOperator(): Boolean {
        val descriptor = resultingDescriptor as? SimpleFunctionDescriptor ?: return false
        return descriptor.fqNameSafe in literalOperatorsFqNames
    }

    private val literalOperatorsFqNames: Set<FqName> = listOf(
        "plus", "minus", "times", "div", "rem", "plus", "minus",
        "times", "div", "rem", "shl", "shr", "ushr", "and", "or",
        "xor", "unaryPlus", "unaryMinus", "inv",
    ).mapTo(mutableSetOf()) { FqName.fromSegments(listOf("kotlin", "Int", it)) }

}

