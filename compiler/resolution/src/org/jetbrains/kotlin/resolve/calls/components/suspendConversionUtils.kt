/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType

fun KotlinResolutionCandidate.getExpectedTypeWithSuspendConversion(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor
): UnwrappedType? {
    if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SuspendConversion)) return null

    if (argument !is SimpleKotlinCallArgument) return null

    val argumentType = argument.receiver.stableType
    if (!argumentType.isFunctionType) return null
    if (argumentType.isSuspendFunctionType) return null

    val parameterType = candidateParameter.type
    if (!parameterType.isSuspendFunctionType) return null

    val nonSuspendParameterType = createFunctionType(
        callComponents.builtIns,
        parameterType.annotations,
        parameterType.getReceiverTypeFromFunctionType(),
        parameterType.getValueParameterTypesFromFunctionType().map { it.type },
        parameterNames = null,
        parameterType.getReturnTypeFromFunctionType(),
        suspendFunction = false
    )

    resolvedCall.registerArgumentWithSuspendConversion(argument, nonSuspendParameterType)

    return nonSuspendParameterType
}