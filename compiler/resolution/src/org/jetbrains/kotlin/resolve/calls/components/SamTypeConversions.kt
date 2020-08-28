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

object SamTypeConversions : ParameterTypeConversion {
    override fun conversionDefinitelyNotNeeded(
        candidate: KotlinResolutionCandidate,
        argument: KotlinCallArgument,
        expectedParameterType: UnwrappedType
    ): Boolean {
        val callComponents = candidate.callComponents
        val generatingAdditionalSamCandidateIsEnabled =
            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
                    !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

        if (generatingAdditionalSamCandidateIsEnabled) return true
        if (expectedParameterType.isNothing()) return true
        if (expectedParameterType.isFunctionType) return true

        val samConversionOracle = callComponents.samConversionOracle
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionForKotlinFunctions)) {
            if (!samConversionOracle.shouldRunSamConversionForFunction(candidate.resolvedCall.candidateDescriptor)) return true
        }

        val declarationDescriptor = expectedParameterType.constructor.declarationDescriptor
        if (declarationDescriptor is ClassDescriptor && declarationDescriptor.isDefinitelyNotSamInterface) return true

        return false
    }

    override fun conversionIsNeededBeforeSubtypingCheck(argument: KotlinCallArgument): Boolean {
        return when (argument) {
            is SubKotlinCallArgument -> {
                val stableType = argument.receiver.stableType
                if (stableType.isFunctionType) return true

                hasNonAnalyzedLambdaAsReturnType(argument.callResult.subResolvedAtoms, stableType)
            }
            is SimpleKotlinCallArgument -> argument.receiver.stableType.isFunctionType
            is LambdaKotlinCallArgument, is CallableReferenceKotlinCallArgument -> true
            else -> false
        }
    }

    private fun hasNonAnalyzedLambdaAsReturnType(subResolvedAtoms: List<ResolvedAtom>?, type: UnwrappedType): Boolean {
        subResolvedAtoms?.forEach {
            if (it is LambdaWithTypeVariableAsExpectedTypeAtom) {
                if (it.expectedType.constructor == type.constructor) return true
            }

            val hasNonAnalyzedLambda = hasNonAnalyzedLambdaAsReturnType(it.subResolvedAtoms, type)
            if (hasNonAnalyzedLambda) return true
        }

        return false
    }

    override fun conversionIsNeededAfterSubtypingCheck(argument: KotlinCallArgument): Boolean {
        return argument is SimpleKotlinCallArgument && argument.receiver.stableType.isFunctionTypeOrSubtype
    }

    override fun convertParameterType(
        candidate: KotlinResolutionCandidate,
        argument: KotlinCallArgument,
        parameter: ParameterDescriptor,
        expectedParameterType: UnwrappedType
    ): UnwrappedType? {
        val callComponents = candidate.callComponents
        val originalExpectedType = argument.getExpectedType(parameter.original, callComponents.languageVersionSettings)

        val convertedTypeByCandidate =
            callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
                expectedParameterType,
                callComponents.samConversionOracle
            ) ?: return null

        val convertedTypeByOriginal =
            if (expectedParameterType.constructor == originalExpectedType.constructor)
                callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
                    originalExpectedType,
                    callComponents.samConversionOracle
                )
            else
                convertedTypeByCandidate

        assert(convertedTypeByCandidate.constructor == convertedTypeByOriginal?.constructor) {
            "If original type is SAM type, then candidate should have same type constructor and corresponding function type\n" +
                    "originalExpectType: $originalExpectedType, candidateExpectType: $expectedParameterType\n" +
                    "functionTypeByOriginal: $convertedTypeByOriginal, functionTypeByCandidate: $convertedTypeByCandidate"
        }

        candidate.resolvedCall.registerArgumentWithSamConversion(
            argument,
            SamConversionDescription(convertedTypeByOriginal!!, convertedTypeByCandidate)
        )

        if (needCompatibilityResolveForSAM(candidate, expectedParameterType)) {
            candidate.markCandidateForCompatibilityResolve()
        }

        val samDescriptor = originalExpectedType.constructor.declarationDescriptor
        if (samDescriptor is ClassDescriptor) {
            callComponents.lookupTracker.record(candidate.scopeTower.location, samDescriptor, SAM_LOOKUP_NAME)
        }

        return convertedTypeByCandidate
    }

    private fun needCompatibilityResolveForSAM(candidate: KotlinResolutionCandidate, typeToConvert: UnwrappedType): Boolean {
        // fun interfaces is a new feature with a new modifier, so no compatibility resolve is needed
        val descriptor = typeToConvert.constructor.declarationDescriptor
        if (descriptor is ClassDescriptor && descriptor.isFun) return false

        // now conversions for Kotlin candidates are possible, so we have to perform compatibility resolve
        return !candidate.callComponents.samConversionOracle.isJavaApplicableCandidate(candidate.resolvedCall.candidateDescriptor)
    }

    fun isJavaParameterCanBeConverted(
        candidate: KotlinResolutionCandidate,
        expectedParameterType: UnwrappedType
    ): Boolean {
        val callComponents = candidate.callComponents

        val samConversionOracle = callComponents.samConversionOracle
        if (!samConversionOracle.isJavaApplicableCandidate(candidate.resolvedCall.candidateDescriptor)) return false

        val declarationDescriptor = expectedParameterType.constructor.declarationDescriptor
        if (declarationDescriptor is ClassDescriptor && declarationDescriptor.isDefinitelyNotSamInterface) return false

        val convertedType =
            callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
                expectedParameterType,
                callComponents.samConversionOracle
            )

        return convertedType != null
    }
}
