/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.types.UnwrappedType

interface ParameterTypeConversion {
    fun conversionDefinitelyNotNeeded(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        expectedParameterType: UnwrappedType
    ): Boolean

    fun conversionIsNeededBeforeSubtypingCheck(argument: KotlinCallArgument, areSuspendOnlySamConversionsSupported: Boolean): Boolean
    fun conversionIsNeededAfterSubtypingCheck(argument: KotlinCallArgument): Boolean

    fun convertParameterType(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        parameter: ParameterDescriptor,
        expectedParameterType: UnwrappedType
    ): UnwrappedType?
}

object TypeConversions {
    fun performCompositeConversionBeforeSubtyping(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
    ): ConversionData {
        val samConversionData = performConversionBeforeSubtyping(
            candidate, argument, candidateParameter, candidateExpectedType, SamTypeConversions
        )

        val suspendConversionData = performConversionBeforeSubtyping(
            candidate, argument, candidateParameter,
            candidateExpectedType = samConversionData.convertedType ?: candidateExpectedType,
            SuspendTypeConversions
        )

        val unitConversionData = performConversionBeforeSubtyping(
            candidate, argument, candidateParameter,
            candidateExpectedType = suspendConversionData.convertedType ?: samConversionData.convertedType ?: candidateExpectedType,
            UnitTypeConversions
        )

        return ConversionData(
            convertedType = unitConversionData.convertedType ?: suspendConversionData.convertedType ?: samConversionData.convertedType,
            wasConversion = samConversionData.wasConversion || suspendConversionData.wasConversion || unitConversionData.wasConversion,
            conversionDefinitelyNotNeeded = samConversionData.conversionDefinitelyNotNeeded &&
                    suspendConversionData.conversionDefinitelyNotNeeded &&
                    unitConversionData.conversionDefinitelyNotNeeded
        )
    }

    fun performCompositeConversionAfterSubtyping(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
    ): UnwrappedType? {
        val samConvertedType = performConversionAfterSubtyping(
            candidate, argument, candidateParameter, candidateExpectedType, SamTypeConversions
        )

        val suspendConvertedType = performConversionAfterSubtyping(
            candidate, argument, candidateParameter,
            candidateExpectedType = samConvertedType ?: candidateExpectedType,
            SuspendTypeConversions
        )

        val unitConvertedType = performConversionAfterSubtyping(
            candidate, argument, candidateParameter,
            candidateExpectedType = suspendConvertedType ?: samConvertedType ?: candidateExpectedType,
            UnitTypeConversions
        )

        return unitConvertedType ?: suspendConvertedType ?: samConvertedType
    }

    private fun performConversionAfterSubtyping(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
        conversion: ParameterTypeConversion
    ): UnwrappedType? {
        return if (
            conversion.conversionIsNeededAfterSubtypingCheck(argument) &&
            !conversion.conversionDefinitelyNotNeeded(candidate, argument, candidateExpectedType)
        ) {
            conversion.convertParameterType(candidate, argument, candidateParameter, candidateExpectedType)
        } else {
            null
        }
    }

    private fun performConversionBeforeSubtyping(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
        conversion: ParameterTypeConversion
    ): ConversionData {
        val conversionDefinitelyNotNeeded = conversion.conversionDefinitelyNotNeeded(candidate, argument, candidateExpectedType)
        val areSuspendOnlySamConversionsSupported =
            candidate.callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SuspendOnlySamConversions)
        return if (
            !conversionDefinitelyNotNeeded &&
            conversion.conversionIsNeededBeforeSubtypingCheck(argument, areSuspendOnlySamConversionsSupported)
        ) {
            ConversionData(
                conversion.convertParameterType(candidate, argument, candidateParameter, candidateExpectedType),
                wasConversion = true,
                conversionDefinitelyNotNeeded
            )
        } else {
            ConversionData(convertedType = null, wasConversion = false, conversionDefinitelyNotNeeded)
        }
    }

    class ConversionData(val convertedType: UnwrappedType?, val wasConversion: Boolean, val conversionDefinitelyNotNeeded: Boolean)
}
