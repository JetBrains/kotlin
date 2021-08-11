/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType

object SuspendTypeConversions : ParameterTypeConversion {
    override fun conversionDefinitelyNotNeeded(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        expectedParameterType: UnwrappedType
    ): Boolean {
        if (argument !is SimpleKotlinCallArgument) return true

        val argumentType = argument.receiver.stableType
        if (argumentType.isSuspendFunctionType) return true

        if (!expectedParameterType.isSuspendFunctionType) return true

        return false
    }

    override fun conversionIsNeededBeforeSubtypingCheck(argument: KotlinCallArgument): Boolean =
        argument is SimpleKotlinCallArgument && argument.receiver.stableType.isFunctionType

    override fun conversionIsNeededAfterSubtypingCheck(argument: KotlinCallArgument): Boolean =
        argument is SimpleKotlinCallArgument && argument.receiver.stableType.isFunctionTypeOrSubtype

    override fun convertParameterType(
        candidate: ResolutionCandidate,
        argument: KotlinCallArgument,
        parameter: ParameterDescriptor,
        expectedParameterType: UnwrappedType
    ): UnwrappedType {
        val nonSuspendParameterType = createFunctionType(
            candidate.callComponents.builtIns,
            expectedParameterType.annotations,
            expectedParameterType.getReceiverTypeFromFunctionType(),
            expectedParameterType.getContextReceiverTypesFromFunctionType(),
            expectedParameterType.getValueParameterTypesFromFunctionType().map { it.type },
            parameterNames = null,
            expectedParameterType.getReturnTypeFromFunctionType(),
            suspendFunction = false
        )

        candidate.resolvedCall.registerArgumentWithSuspendConversion(argument, nonSuspendParameterType)

        candidate.markCandidateForCompatibilityResolve()

        return nonSuspendParameterType
    }
}
