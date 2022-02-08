/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.resolve.calls.components.ArgumentsToCandidateParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.CheckArgumentsInParenthesis
import org.jetbrains.kotlin.resolve.calls.components.CheckCallableReference
import org.jetbrains.kotlin.resolve.calls.components.CheckContextReceiversResolutionPart
import org.jetbrains.kotlin.resolve.calls.components.CheckExplicitReceiverKindConsistency
import org.jetbrains.kotlin.resolve.calls.components.CheckExternalArgument
import org.jetbrains.kotlin.resolve.calls.components.CheckInfixResolutionPart
import org.jetbrains.kotlin.resolve.calls.components.CheckOperatorResolutionPart
import org.jetbrains.kotlin.resolve.calls.components.CheckReceivers
import org.jetbrains.kotlin.resolve.calls.components.CheckSuperExpressionCallPart
import org.jetbrains.kotlin.resolve.calls.components.CheckVisibility
import org.jetbrains.kotlin.resolve.calls.components.CollectionTypeVariableUsagesInfo
import org.jetbrains.kotlin.resolve.calls.components.CompatibilityOfPartiallyApplicableSamConversion
import org.jetbrains.kotlin.resolve.calls.components.CompatibilityOfTypeVariableAsIntersectionTypePart
import org.jetbrains.kotlin.resolve.calls.components.CreateFreshVariablesSubstitutor
import org.jetbrains.kotlin.resolve.calls.components.EagerResolveOfCallableReferences
import org.jetbrains.kotlin.resolve.calls.components.MapArguments
import org.jetbrains.kotlin.resolve.calls.components.MapTypeArguments
import org.jetbrains.kotlin.resolve.calls.components.NoArguments
import org.jetbrains.kotlin.resolve.calls.components.NoTypeArguments
import org.jetbrains.kotlin.resolve.calls.components.PostponedVariablesInitializerResolutionPart

enum class KotlinCallKind(vararg resolutionPart: ResolutionPart) {
    VARIABLE(
        CheckVisibility,
        CheckSuperExpressionCallPart,
        NoTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
        CollectionTypeVariableUsagesInfo,
        CheckExplicitReceiverKindConsistency,
        CheckReceivers,
        PostponedVariablesInitializerResolutionPart,
        CheckContextReceiversResolutionPart
    ),
    FUNCTION(
        CheckVisibility,
        CheckInfixResolutionPart,
        CheckOperatorResolutionPart,
        CheckSuperExpressionCallPart,
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
        CollectionTypeVariableUsagesInfo,
        CheckExplicitReceiverKindConsistency,
        CheckReceivers,
        CheckArgumentsInParenthesis,
        CheckExternalArgument,
        EagerResolveOfCallableReferences,
        CompatibilityOfTypeVariableAsIntersectionTypePart,
        CompatibilityOfPartiallyApplicableSamConversion,
        PostponedVariablesInitializerResolutionPart,
        CheckContextReceiversResolutionPart
    ),
    INVOKE(*FUNCTION.resolutionSequence.toTypedArray()),
    CALLABLE_REFERENCE(
        CheckVisibility,
        NoTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
        CollectionTypeVariableUsagesInfo,
        CheckReceivers,
        CheckCallableReference,
        CompatibilityOfTypeVariableAsIntersectionTypePart
    ),
    UNSUPPORTED();

    val resolutionSequence = resolutionPart.asList()
}