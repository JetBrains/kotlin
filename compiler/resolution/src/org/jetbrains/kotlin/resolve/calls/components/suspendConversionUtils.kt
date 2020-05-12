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

object SuspendTypeConversions {
    fun conversionDefinitelyNotNeeded(
        candidate: KotlinResolutionCandidate,
        argument: KotlinCallArgument,
        candidateParameter: ParameterDescriptor
    ): Boolean {
        if (!candidate.callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SuspendConversion)) return true

        if (argument !is SimpleKotlinCallArgument) return true

        val argumentType = argument.receiver.stableType
        if (!argumentType.isFunctionType) return true
        if (argumentType.isSuspendFunctionType) return true

        if (!candidateParameter.type.isSuspendFunctionType) return true

        return false
    }

    fun KotlinResolutionCandidate.conversionMightBeNeededBeforeSubtypingCheck(): Boolean = true
    fun KotlinResolutionCandidate.conversionMightBeNeededAfterSubtypingCheck(): Boolean = false
}


fun KotlinResolutionCandidate.getExpectedTypeWithSuspendConversion(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor
): UnwrappedType? {
    if (SuspendTypeConversions.conversionDefinitelyNotNeeded(this, argument, candidateParameter)) return null

    val parameterType = candidateParameter.type
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