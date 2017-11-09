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
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.NewOverloadingConflictResolver
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.types.UnwrappedType
import java.lang.UnsupportedOperationException


class KotlinCallResolver(
        private val towerResolver: TowerResolver,
        private val kotlinCallCompleter: KotlinCallCompleter,
        private val overloadingConflictResolver: NewOverloadingConflictResolver,
        private val callComponents: KotlinCallComponents
) {

    fun resolveCall(
            scopeTower: ImplicitScopeTower,
            resolutionCallbacks: KotlinResolutionCallbacks,
            kotlinCall: KotlinCall,
            expectedType: UnwrappedType?,
            factoryProviderForInvoke: CandidateFactoryProviderForInvoke<KotlinResolutionCandidate>,
            collectAllCandidates: Boolean
    ): CallResolutionResult {
        kotlinCall.checkCallInvariants()

        val candidateFactory = SimpleCandidateFactory(callComponents, scopeTower, kotlinCall)
        val processor = when(kotlinCall.callKind) {
            KotlinCallKind.VARIABLE -> {
                createVariableAndObjectProcessor(scopeTower, kotlinCall.name, candidateFactory, kotlinCall.explicitReceiver?.receiver)
            }
            KotlinCallKind.FUNCTION -> {
                createFunctionProcessor(scopeTower, kotlinCall.name, candidateFactory, factoryProviderForInvoke, kotlinCall.explicitReceiver?.receiver)
            }
            KotlinCallKind.UNSUPPORTED -> throw UnsupportedOperationException()
        }

        if (collectAllCandidates) {
            val allCandidates = towerResolver.collectAllCandidates(scopeTower, processor, kotlinCall.name)
            return kotlinCallCompleter.createAllCandidatesResult(allCandidates, expectedType, resolutionCallbacks)
        }

        val candidates = towerResolver.runResolve(scopeTower, processor, useOrder = kotlinCall.callKind != KotlinCallKind.UNSUPPORTED, name = kotlinCall.name)

        return choseMostSpecific(candidateFactory, resolutionCallbacks, expectedType, candidates)
    }

    fun resolveGivenCandidates(
            scopeTower: ImplicitScopeTower,
            resolutionCallbacks: KotlinResolutionCallbacks,
            kotlinCall: KotlinCall,
            expectedType: UnwrappedType?,
            givenCandidates: Collection<GivenCandidate>,
            collectAllCandidates: Boolean
    ): CallResolutionResult {
        kotlinCall.checkCallInvariants()
        val candidateFactory = SimpleCandidateFactory(callComponents, scopeTower, kotlinCall)

        val resolutionCandidates = givenCandidates.map { candidateFactory.createCandidate(it).forceResolution() }

        if (collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(KnownResultProcessor(resolutionCandidates),
                                                                    TowerResolver.AllCandidatesCollector(),
                                                                    useOrder = false)
            return kotlinCallCompleter.createAllCandidatesResult(allCandidates, expectedType, resolutionCallbacks)

        }
        val candidates = towerResolver.runWithEmptyTowerData(KnownResultProcessor(resolutionCandidates),
                                                             TowerResolver.SuccessfulResultCollector(),
                                                             useOrder = true)
        return choseMostSpecific(candidateFactory, resolutionCallbacks, expectedType, candidates)
    }

    private fun choseMostSpecific(
            candidateFactory: SimpleCandidateFactory,
            resolutionCallbacks: KotlinResolutionCallbacks,
            expectedType: UnwrappedType?,
            candidates: Collection<KotlinResolutionCandidate>
    ): CallResolutionResult {
        val isDebuggerContext = candidateFactory.scopeTower.isDebuggerContext

        val maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
                candidates,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                discriminateGenerics = true, // todo
                isDebuggerContext = isDebuggerContext)

        return kotlinCallCompleter.runCompletion(candidateFactory, maximallySpecificCandidates, expectedType, resolutionCallbacks)
    }
}

