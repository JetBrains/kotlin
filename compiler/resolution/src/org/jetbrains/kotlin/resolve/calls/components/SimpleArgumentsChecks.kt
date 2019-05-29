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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ReceiverConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.NotNullTypeVariable
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.captureFromExpression
import org.jetbrains.kotlin.types.checker.hasSupertypeWithGivenTypeConstructor
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.types.upperIfFlexible


fun checkSimpleArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: SimpleKotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
): ResolvedAtom = when (argument) {
    is ExpressionKotlinCallArgument -> checkExpressionArgument(csBuilder, argument, expectedType, diagnosticsHolder, isReceiver)
    is SubKotlinCallArgument -> checkSubCallArgument(csBuilder, argument, expectedType, diagnosticsHolder, isReceiver)
    else -> unexpectedArgument(argument)
}

private fun checkExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    expressionArgument: ExpressionKotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
): ResolvedAtom {
    val resolvedKtExpression = ResolvedExpressionAtom(expressionArgument)
    if (expectedType == null) return resolvedKtExpression

    // todo run this approximation only once for call
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(expressionArgument.receiver.stableType, expectedType)

    fun unstableSmartCastOrSubtypeError(
        unstableType: UnwrappedType?, actualExpectedType: UnwrappedType, position: ConstraintPosition
    ): KotlinCallDiagnostic? {
        if (unstableType != null) {
            if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, actualExpectedType, position)) {
                return UnstableSmartCast(expressionArgument, unstableType)
            }
        }

        if (argumentType.isMarkedNullable) {
            if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, actualExpectedType, position)) return null
            if (csBuilder.addSubtypeConstraintIfCompatible(argumentType.makeNotNullable(), actualExpectedType, position)) {
                return ArgumentTypeMismatchDiagnostic(actualExpectedType, argumentType, expressionArgument)
            }
        }

        csBuilder.addSubtypeConstraint(argumentType, actualExpectedType, position)
        return null
    }

    val expectedNullableType = expectedType.makeNullableAsSpecified(true)
    val position = if (isReceiver) ReceiverConstraintPosition(expressionArgument) else ArgumentConstraintPosition(expressionArgument)

    // Used only for arguments with @NotNull annotation
    if (expectedType is NotNullTypeVariable && argumentType.isMarkedNullable) {
        diagnosticsHolder.addDiagnostic(ArgumentTypeMismatchDiagnostic(expectedType, argumentType, expressionArgument))
    }

    if (expressionArgument.isSafeCall) {
        if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            diagnosticsHolder.addDiagnosticIfNotNull(
                unstableSmartCastOrSubtypeError(expressionArgument.receiver.unstableType, expectedNullableType, position)
            )
        }
        return resolvedKtExpression
    }

    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            diagnosticsHolder.addDiagnosticIfNotNull(
                unstableSmartCastOrSubtypeError(
                    expressionArgument.receiver.unstableType,
                    expectedType,
                    position
                )
            )
            return resolvedKtExpression
        }

        val unstableType = expressionArgument.receiver.unstableType
        if (unstableType != null && csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
            diagnosticsHolder.addDiagnostic(UnstableSmartCast(expressionArgument, unstableType))
        } else if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            diagnosticsHolder.addDiagnostic(UnsafeCallError(expressionArgument))
        } else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
        }
    }

    return resolvedKtExpression
}

/**
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val xr = bar(x)
 * }
 * Here we try to capture from upper bound from type parameter.
 * We replace type of `x` to `Inv<out Int>`(we chose supertype which contains supertype with expectedTypeConstructor) and capture from this type.
 * It is correct, because it is like this code:
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val inv: Inv<out Int> = x
 *      val xr = bar(inv)
 * }
 *
 */
private fun captureFromTypeParameterUpperBoundIfNeeded(argumentType: UnwrappedType, expectedType: UnwrappedType): UnwrappedType {
    val expectedTypeConstructor = expectedType.upperIfFlexible().constructor

    if (argumentType.lowerIfFlexible().constructor.declarationDescriptor is TypeParameterDescriptor) {
        val chosenSupertype = argumentType.lowerIfFlexible().supertypes().singleOrNull {
            it.constructor.declarationDescriptor is ClassifierDescriptorWithTypeParameters &&
                    it.unwrap().hasSupertypeWithGivenTypeConstructor(expectedTypeConstructor)
        }
        if (chosenSupertype != null) {
            return captureFromExpression(chosenSupertype.unwrap()) ?: argumentType
        }
    }

    return argumentType
}

private fun checkSubCallArgument(
    csBuilder: ConstraintSystemBuilder,
    subCallArgument: SubKotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
): ResolvedAtom {
    val subCallResult = subCallArgument.callResult

    if (expectedType == null) return subCallResult

    val expectedNullableType = expectedType.makeNullableAsSpecified(true)
    val position = if (isReceiver) ReceiverConstraintPosition(subCallArgument) else ArgumentConstraintPosition(subCallArgument)

    // subArgument cannot has stable smartcast
    // return type can contains fixed type variables
    val currentReturnType =
        (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor)
            .safeSubstitute(subCallArgument.receiver.receiverValue.type.unwrap())
    if (subCallArgument.isSafeCall) {
        csBuilder.addSubtypeConstraint(currentReturnType, expectedNullableType, position)
        return subCallResult
    }

    if (isReceiver && !csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedType, position) &&
        csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedNullableType, position)
    ) {
        diagnosticsHolder.addDiagnostic(UnsafeCallError(subCallArgument))
        return subCallResult
    }

    csBuilder.addSubtypeConstraint(currentReturnType, expectedType, position)
    return subCallResult
}

