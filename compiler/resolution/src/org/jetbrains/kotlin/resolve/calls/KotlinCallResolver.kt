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

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.resolve.calls.components.KotlinCallCompleter
import org.jetbrains.kotlin.resolve.calls.components.NewOverloadingConflictResolver
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.types.UnwrappedType
import java.lang.UnsupportedOperationException


class KotlinCallResolver(
        private val towerResolver: TowerResolver,
        private val kotlinCallCompleter: KotlinCallCompleter,
        private val overloadingConflictResolver: NewOverloadingConflictResolver
) {

    fun resolveCall(
            callContext: KotlinCallContext,
            kotlinCall: KotlinCall,
            expectedType: UnwrappedType?,
            factoryProviderForInvoke: CandidateFactoryProviderForInvoke<KotlinResolutionCandidate>
    ): Collection<ResolvedKotlinCall> {
        val scopeTower = callContext.scopeTower

        kotlinCall.checkCallInvariants()

        val candidateFactory = SimpleCandidateFactory(callContext, kotlinCall)
        val processor = when(kotlinCall.callKind) {
            KotlinCallKind.VARIABLE -> {
                createVariableAndObjectProcessor(scopeTower, kotlinCall.name, candidateFactory, kotlinCall.explicitReceiver?.receiver)
            }
            KotlinCallKind.FUNCTION -> {
                createFunctionProcessor(scopeTower, kotlinCall.name, candidateFactory, factoryProviderForInvoke, kotlinCall.explicitReceiver?.receiver)
            }
            KotlinCallKind.UNSUPPORTED -> throw UnsupportedOperationException()
        }

        val candidates = towerResolver.runResolve(scopeTower, processor, useOrder = kotlinCall.callKind != KotlinCallKind.UNSUPPORTED)

        return choseMostSpecific(callContext, expectedType, candidates)
    }

    fun resolveGivenCandidates(
            callContext: KotlinCallContext,
            kotlinCall: KotlinCall,
            expectedType: UnwrappedType?,
            givenCandidates: Collection<GivenCandidate>
    ): Collection<ResolvedKotlinCall> {
        kotlinCall.checkCallInvariants()

        val resolutionCandidates = givenCandidates.map {
            SimpleKotlinResolutionCandidate(callContext,
                                            kotlinCall,
                                            if (it.dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER,
                                            it.dispatchReceiver?.let { ReceiverExpressionKotlinCallArgument(it) },
                                            null,
                                            it.descriptor,
                                            it.knownTypeParametersResultingSubstitutor,
                                            listOf()
            )
        }
        val candidates = towerResolver.runWithEmptyTowerData(KnownResultProcessor(resolutionCandidates),
                                                             TowerResolver.SuccessfulResultCollector { it.status },
                                                             useOrder = true)
        return choseMostSpecific(callContext, expectedType, candidates)
    }

    private fun choseMostSpecific(
            callContext: KotlinCallContext,
            expectedType: UnwrappedType?,
            candidates: Collection<KotlinResolutionCandidate>
    ): Collection<ResolvedKotlinCall> {

        val maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
                candidates,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                discriminateGenerics = true, // todo
                isDebuggerContext = callContext.scopeTower.isDebuggerContext)

        val singleResult = maximallySpecificCandidates.singleOrNull()?.let {
            kotlinCallCompleter.completeCallIfNecessary(it, expectedType, callContext.resolutionCallbacks)
        }
        if (singleResult != null) {
            return listOf(singleResult)
        }

        return maximallySpecificCandidates.map {
            kotlinCallCompleter.transformWhenAmbiguity(it, callContext.resolutionCallbacks)
        }
    }
}

