/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

object NewSchemeOfIntegerOperatorResolutionChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ApproximateIntegerLiteralTypesInReceiverPosition)) return
        for ((valueParameter, arguments) in resolvedCall.valueArguments) {
            val expectedType = if (valueParameter.isVararg) {
                valueParameter.varargElementType ?: continue
            } else {
                valueParameter.type
            }.unwrap().lowerIfFlexible()
            if (!expectedType.isPrimitiveNumberOrNullableType()) {
                continue
            }
            for (argument in arguments.arguments) {
                val expression = KtPsiUtil.deparenthesize(argument.getArgumentExpression()) ?: continue
                checkArgumentImpl(expectedType, expression, context.trace, context.moduleDescriptor)
            }
        }
    }

    @JvmStatic
    fun checkArgument(
        expectedType: KotlinType,
        argument: KtExpression,
        languageVersionSettings: LanguageVersionSettings,
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (languageVersionSettings.supportsFeature(LanguageFeature.ApproximateIntegerLiteralTypesInReceiverPosition)) return
        val type = expectedType.lowerIfFlexible()
        if (type.isPrimitiveNumberOrNullableType()) {
            checkArgumentImpl(type, KtPsiUtil.deparenthesize(argument)!!, trace, moduleDescriptor)
        }
    }

    private fun checkArgumentImpl(
        expectedType: SimpleType,
        argumentExpression: KtExpression,
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        val bindingContext = trace.bindingContext
        val callForArgument = argumentExpression.getResolvedCall(bindingContext) ?: return
        if (!callForArgument.isIntOperator()) return
        val callElement = callForArgument.call.callElement as? KtExpression ?: return
        val deparenthesizedElement = KtPsiUtil.deparenthesize(callElement)!!
        if (deparenthesizedElement is KtConstantExpression) return
        if (deparenthesizedElement is KtUnaryExpression) {
            val token = deparenthesizedElement.operationToken
            if (token == KtTokens.PLUS || token == KtTokens.MINUS) return
        }

        val compileTimeValue = bindingContext[BindingContext.COMPILE_TIME_VALUE, argumentExpression] ?: return

        val newExpressionType = when (compileTimeValue) {
            is IntegerValueTypeConstant -> {
                val currentExpressionType = compileTimeValue.unknownIntegerType
                val valueTypeConstructor = currentExpressionType.constructor as? IntegerLiteralTypeConstructor ?: return
                valueTypeConstructor.getApproximatedType()
            }
            is TypedCompileTimeConstant -> {
                val typeFromCall = callForArgument.resultingDescriptor.returnType?.lowerIfFlexible()
                if (typeFromCall != null) {
                    typeFromCall
                } else {
                    val constantValue = compileTimeValue.constantValue
                    if (constantValue is ErrorValue) return
                    // Values of all numeric constants are held in Long value
                    val value = constantValue.value as? Long ?: return
                    IntegerLiteralTypeConstructor(value, moduleDescriptor, compileTimeValue.parameters).getApproximatedType()
                }
            }
            else -> return
        }
        if (newExpressionType.constructor != expectedType.constructor) {
            val willBeConversion = newExpressionType.isInt() && expectedType.makeNotNullable().isLong()
            if (!willBeConversion) {
                trace.report(Errors.INTEGER_OPERATOR_RESOLVE_WILL_CHANGE.on(argumentExpression, newExpressionType))
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

