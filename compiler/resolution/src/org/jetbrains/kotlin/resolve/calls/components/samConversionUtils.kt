/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForPossibleSamType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.isNothing

object SamTypeConversions {
    fun conversionMightBeNeededBeforeSubtypingCheck(argument: KotlinCallArgument): Boolean {
        return when (argument) {
            is SimpleKotlinCallArgument -> argument.receiver.stableType.isFunctionType
            is LambdaKotlinCallArgument, is CallableReferenceKotlinCallArgument -> true
            else -> false
        }
    }

    fun conversionMightBeNeededAfterSubtypingCheck(argument: KotlinCallArgument): Boolean {
        return argument is SimpleKotlinCallArgument && argument.receiver.stableType.isFunctionTypeOrSubtype
    }


    fun conversionDefinitelyNotNeeded(
        candidate: KotlinResolutionCandidate,
        candidateParameter: ParameterDescriptor
    ): Boolean {
        val callComponents = candidate.callComponents
        val generatingAdditionalSamCandidateIsEnabled =
            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
                    !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

        if (generatingAdditionalSamCandidateIsEnabled) return true
        if (candidateParameter.type.isNothing()) return true

        val samConversionOracle = callComponents.samConversionOracle
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionForKotlinFunctions)) {
            if (!samConversionOracle.shouldRunSamConversionForFunction(candidate.resolvedCall.candidateDescriptor)) return true
        }

        return false
    }
}

fun KotlinResolutionCandidate.getExpectedTypeWithSAMConversion(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor
): UnwrappedType? {
    if (SamTypeConversions.conversionDefinitelyNotNeeded(this, candidateParameter)) return null

    val argumentIsFunctional = when (argument) {
        is SimpleKotlinCallArgument -> argument.receiver.stableType.isFunctionType
        is LambdaKotlinCallArgument, is CallableReferenceKotlinCallArgument -> true
        else -> false
    }
    if (!argumentIsFunctional) return null

    val originalExpectedType = argument.getExpectedType(candidateParameter.original, callComponents.languageVersionSettings)

    val convertedTypeByOriginal =
        callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
            originalExpectedType,
            callComponents.samConversionOracle
        ) ?: return null

    val candidateExpectedType = argument.getExpectedType(candidateParameter, callComponents.languageVersionSettings)
    val convertedTypeByCandidate =
        callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
            candidateExpectedType,
            callComponents.samConversionOracle
        )

    assert(candidateExpectedType.constructor == originalExpectedType.constructor && convertedTypeByCandidate != null) {
        "If original type is SAM type, then candidate should have same type constructor and corresponding function type\n" +
                "originalExpectType: $originalExpectedType, candidateExpectType: $candidateExpectedType\n" +
                "functionTypeByOriginal: $convertedTypeByOriginal, functionTypeByCandidate: $convertedTypeByCandidate"
    }

    resolvedCall.registerArgumentWithSamConversion(argument, SamConversionDescription(convertedTypeByOriginal, convertedTypeByCandidate!!))

    val samDescriptor = originalExpectedType.constructor.declarationDescriptor
    if (samDescriptor is ClassDescriptor) {
        callComponents.lookupTracker.record(scopeTower.location, samDescriptor, SAM_LOOKUP_NAME)
    }

    return convertedTypeByCandidate
}
