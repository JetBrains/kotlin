/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors.PROGRESSIONS_CHANGING_RESOLVE
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.KotlinCallResolver
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.SimpleKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.ClassicTypeCheckerState
import org.jetbrains.kotlin.types.checker.ClassicTypeCheckerStateInternals
import org.jetbrains.kotlin.types.checker.intersectTypes
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

/*
 NB: this checker is exceptionally temporary added for stdlib migration purposes (see KT-49276).
 Please don't use similar logic for other checkers
 */
@OptIn(ClassicTypeCheckerStateInternals::class)
class PassingProgressionAsCollectionCallChecker(private val kotlinCallResolver: KotlinCallResolver) {
    private val typeCheckerState = ClassicTypeCheckerState(isErrorTypeEqualsToAnything = false)

    private val iterableProgressions = listOf(
        CHAR_RANGE_FQN, CHAR_PROGRESSION_FQN,
        INT_RANGE_FQN, INT_PROGRESSION_FQN,
        LONG_RANGE_FQN, LONG_PROGRESSION_FQN,
        UINT_RANGE_FQN, UINT_PROGRESSION_FQN,
        ULONG_RANGE_FQN, ULONG_PROGRESSION_FQN
    )

    private fun check(
        resolvedCall: ResolvedCall<*>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext,
    ) {
        // The stdlib migration is going to be finished in 1.8, checks aren't needed there (DisableCheckingChangedProgressionsResolve has 1.8 since version)
        val isCheckingDisabled = context.languageVersionSettings.supportsFeature(LanguageFeature.DisableCheckingChangedProgressionsResolve)

        if (isCheckingDisabled || resolvedCall !is NewResolvedCallImpl<*>) return

        val kotlinCall = resolvedCall.psiKotlinCall
        val valueArguments = kotlinCall.argumentsInParenthesis.takeIf { it.isNotEmpty() } ?: return

        val progressionOrRangeArgumentTypes = valueArguments.map {
            if (it !is SimpleKotlinCallArgument) return@map null
            getRangeOrProgressionElementType(it.receiver.receiverValue.type, iterableProgressions)
        }

        if (progressionOrRangeArgumentTypes.all { it == null }) return

        val builtIns = resolvedCall.candidateDescriptor.builtIns
        val newArguments = replaceArgumentsWithCollectionIfNeeded(valueArguments, progressionOrRangeArgumentTypes, context.trace, builtIns)

        val newCall = PSIKotlinCallImpl(
            kotlinCall.callKind, kotlinCall.psiCall, kotlinCall.tracingStrategy, kotlinCall.explicitReceiver,
            kotlinCall.dispatchReceiverForInvokeExtension, kotlinCall.name, kotlinCall.typeArguments, newArguments,
            kotlinCall.externalArgument, kotlinCall.startingDataFlowInfo, kotlinCall.resultDataFlowInfo,
            kotlinCall.dataFlowInfoForArguments, kotlinCall.isForImplicitInvoke
        )

        val candidateForCollectionReplacedArgument = kotlinCallResolver.resolveCall(
            scopeTower, resolutionCallbacks, newCall, expectedType, context.collectAllCandidates
        ).singleOrNull() ?: return

        // Resolve wasn't changed or inapplicable
        if (
            candidateForCollectionReplacedArgument.descriptor == resolvedCall.candidateDescriptor ||
            !candidateForCollectionReplacedArgument.isSuccessful
        ) return

        val collectionOfAnyType = makeCollectionOfAnyType(builtIns)

        for ((i, argument) in newCall.argumentsInParenthesis.withIndex()) {
            // Skip if the argument wasn't a Range/Progression
            if (progressionOrRangeArgumentTypes.getOrNull(i) == null) continue

            val resolvedCallForCollectionReplacedArgument = candidateForCollectionReplacedArgument.resolvedCall
            val alternativeParameterType =
                resolvedCallForCollectionReplacedArgument.argumentToCandidateParameter[argument]?.type?.let { type ->
                    if (type.isTypeParameter()) {
                        val typeVariable = resolvedCallForCollectionReplacedArgument.freshVariablesSubstitutor.freshVariables.find {
                            it.originalTypeParameter == type.constructor.declarationDescriptor
                        }?.freshTypeConstructor ?: return@let null
                        resolutionCallbacks.findResultType(candidateForCollectionReplacedArgument.getSystem(), typeVariable)
                    } else type
                } ?: continue

            val cons = alternativeParameterType.constructor

            if (cons.declarationDescriptor != builtIns.collection && (cons !is IntersectionTypeConstructor || cons.supertypes.none { it.constructor.declarationDescriptor == builtIns.collection })) continue

            val argumentExpression = argument.psiExpression ?: continue
            val initialArgumentType = resolvedCall.candidateDescriptor.valueParameters.getOrNull(i)?.type ?: continue

            // Iterable initial type is an exception, considered as similar to Collection passing candidate
            if (initialArgumentType.constructor.declarationDescriptor == builtIns.iterable) continue

            // The initial type should be wider than Collection
            if (
                AbstractTypeChecker.isSubtypeOf(typeCheckerState, collectionOfAnyType, initialArgumentType)
                || initialArgumentType.isTypeParameter()
            ) {
                context.trace.report(
                    PROGRESSIONS_CHANGING_RESOLVE.on(
                        candidateForCollectionReplacedArgument.callComponents.languageVersionSettings,
                        argumentExpression,
                        resolvedCallForCollectionReplacedArgument.candidateDescriptor
                    )
                )
            }
        }
    }

    fun check(
        overloadResolutionResults: OverloadResolutionResults<*>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext,
    ) {
        if (!overloadResolutionResults.isSingleResult) return

        val resolvedCall = overloadResolutionResults.resultingCall as? NewAbstractResolvedCall<*> ?: return

        check(resolvedCall, scopeTower, resolutionCallbacks, expectedType, context)
    }

    private fun makeCollectionOfAnyType(builtIns: KotlinBuiltIns): KotlinType =
        KotlinTypeFactory.simpleNotNullType(
            TypeAttributes.Empty,
            builtIns.collection,
            listOf(TypeProjectionImpl(builtIns.nullableAnyType))
        )

    private fun replaceArgumentsWithCollectionIfNeeded(
        valueArguments: List<KotlinCallArgument>,
        progressionOrRangeArgumentTypes: List<KotlinType?>,
        trace: BindingTrace,
        builtIns: KotlinBuiltIns
    ): List<KotlinCallArgument> = valueArguments.mapIndexed { i, argument ->
        if (argument !is ExpressionKotlinCallArgumentImpl) return@mapIndexed argument
        val progressionOrRangeElementType = progressionOrRangeArgumentTypes[i] ?: return@mapIndexed argument
        val psiExpression = argument.psiExpression ?: return@mapIndexed argument
        val newType = intersectTypes(
            listOf(
                KotlinTypeFactory.simpleNotNullType(
                    TypeAttributes.Empty,
                    builtIns.collection,
                    listOf(TypeProjectionImpl(progressionOrRangeElementType))
                ),
                argument.receiver.receiverValue.type.unwrap()
            )
        )

        ExpressionKotlinCallArgumentImpl(
            argument.psiCallArgument.valueArgument,
            DataFlowInfo.EMPTY,
            DataFlowInfo.EMPTY,
            ReceiverValueWithSmartCastInfo(
                ExpressionReceiver.create(psiExpression, newType, trace.bindingContext),
                emptySet(),
                true
            )
        )
    }
}
