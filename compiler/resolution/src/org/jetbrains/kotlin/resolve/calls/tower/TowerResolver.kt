/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.yieldIfNotNull
import java.util.*
import kotlin.coroutines.experimental.buildSequence

interface Candidate {
    // this operation should be very fast
    val isSuccessful: Boolean

    val resultingApplicability: ResolutionCandidateApplicability
}

interface CandidateFactory<out C: Candidate> {
    fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): C
}

interface CandidateFactoryProviderForInvoke<C : Candidate> {

    // variable here is resolved, invoke -- only chosen
    fun transformCandidate(variable: C, invoke: C): C

    fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<C>

    // foo() -> ReceiverValue(foo), context for invoke
    // null means that there is no invoke on variable
    fun factoryForInvoke(variable: C, useExplicitReceiver: Boolean): Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<C>>?
}

sealed class TowerData {
    object Empty : TowerData()
    class OnlyImplicitReceiver(val implicitReceiver: ReceiverValueWithSmartCastInfo): TowerData()
    class TowerLevel(val level: ScopeTowerLevel) : TowerData()
    class BothTowerLevelAndImplicitReceiver(val level: ScopeTowerLevel, val implicitReceiver: ReceiverValueWithSmartCastInfo) : TowerData()
}

interface ScopeTowerProcessor<out C> {
    // Candidates with matched receivers (dispatch receiver was already matched in ScopeTowerLevel)
    // Candidates in one groups have same priority, first group has highest priority.
    fun process(data: TowerData): List<Collection<C>>
}

interface SimpleScopeTowerProcessor<out C> : ScopeTowerProcessor<C> {
    fun simpleProcess(data: TowerData): Collection<C>

    override fun process(data: TowerData): List<Collection<C>> = listOfNotNull(simpleProcess(data).takeIf { it.isNotEmpty() })
}

class TowerResolver {
    fun <C: Candidate> runResolve(
            scopeTower: ImplicitScopeTower,
            processor: ScopeTowerProcessor<C>,
            useOrder: Boolean,
            name: Name
    ): Collection<C> = scopeTower.run(processor, SuccessfulResultCollector(), useOrder, name)

    fun <C: Candidate> collectAllCandidates(
            scopeTower: ImplicitScopeTower,
            processor: ScopeTowerProcessor<C>,
            name: Name
    ): Collection<C>
            = scopeTower.run(processor, AllCandidatesCollector(), false, name)

    private fun ImplicitScopeTower.createNonLocalLevels(name: Name): Sequence<ScopeTowerLevel> = buildSequence {
        lexicalScope.parentsWithSelf.forEach { scope ->
            if (scope is LexicalScope) {
                if (!scope.kind.withLocalDescriptors && scope.mayFitForName(name, location)) {
                    yield(ScopeBasedTowerLevel(this@createNonLocalLevels, scope))
                }

                getImplicitReceiver(scope)?.let {
                    yieldIfNotNull(MemberScopeTowerLevel(this@createNonLocalLevels, it).takeIf { it.mayFitForName(name) })
                }
            }
            else if (scope.mayFitForName(name, location)) {
                yield(ImportingScopeBasedTowerLevel(this@createNonLocalLevels, scope as ImportingScope))
            }
        }
    }

    private fun <C : Candidate> ImplicitScopeTower.run(
            processor: ScopeTowerProcessor<C>,
            resultCollector: ResultCollector<C>,
            useOrder: Boolean,
            name: Name
    ): Collection<C> {
        fun TowerData.process() = processTowerData(processor, resultCollector, useOrder, this)

        // Lazy calculation
        var nonLocalLevels: Collection<ScopeTowerLevel>? = null
        val hidesMembersLevel = HidesMembersTowerLevel(this)
        val syntheticLevel = SyntheticScopeBasedTowerLevel(this, syntheticScopes)

        if (name in HIDES_MEMBERS_NAME_LIST) {
            // hides members extensions for explicit receiver
            TowerData.TowerLevel(hidesMembersLevel).process()?.let { return it }
        }

        // possibly there is explicit member
        TowerData.Empty.process()?.let { return it }
        // synthetic property for explicit receiver
        TowerData.TowerLevel(syntheticLevel).process()?.let { return it }

        val localLevels =
                lexicalScope.parentsWithSelf.
                        filterIsInstance<LexicalScope>().filter { it.kind.withLocalDescriptors && it.mayFitForName(name, location) }.
                        map { ScopeBasedTowerLevel(this@run, it) }.toList()

        // local non-extensions or extension for explicit receiver
        for (localLevel in localLevels) {
            TowerData.TowerLevel(localLevel).process()?.let { return it }
        }

        for (scope in lexicalScope.parentsWithSelf) {
            if (scope is LexicalScope) {
                // statics
                if (!scope.kind.withLocalDescriptors && scope.mayFitForName(name, location)) {
                    TowerData.TowerLevel(ScopeBasedTowerLevel(this, scope)).process()?.let { return it }
                }

                val implicitReceiver = getImplicitReceiver(scope)
                if (implicitReceiver != null) {
                    if (name in HIDES_MEMBERS_NAME_LIST) {
                        // hides members extensions
                        TowerData.BothTowerLevelAndImplicitReceiver(hidesMembersLevel, implicitReceiver).process()?.let { return it }
                    }

                    // members of implicit receiver or member extension for explicit receiver
                    MemberScopeTowerLevel(this, implicitReceiver).takeIf { it.mayFitForName(name) }?.let {
                        TowerData.TowerLevel(it).process()?.let { return it }
                    }

                    // synthetic properties
                    TowerData.BothTowerLevelAndImplicitReceiver(syntheticLevel, implicitReceiver).process()?.let { return it }

                    // invokeExtension on local variable
                    TowerData.OnlyImplicitReceiver(implicitReceiver).process()?.let { return it }

                    // local extensions for implicit receiver
                    for (localLevel in localLevels) {
                        TowerData.BothTowerLevelAndImplicitReceiver(localLevel, implicitReceiver).process()?.let { return it }
                    }

                    // extension for implicit receiver
                    if (nonLocalLevels == null) {
                        nonLocalLevels = arrayListOf()
                        for (nonLocalLevel in createNonLocalLevels(name)) {
                            TowerData.BothTowerLevelAndImplicitReceiver(nonLocalLevel, implicitReceiver).process()?.let { return it }
                            nonLocalLevels.add(nonLocalLevel)
                        }
                    }
                    else {
                        for (nonLocalLevel in nonLocalLevels) {
                            TowerData.BothTowerLevelAndImplicitReceiver(nonLocalLevel, implicitReceiver).process()?.let { return it }
                        }
                    }
                }
            }
            else if (scope.mayFitForName(name, location)) {
                // functions with no receiver or extension for explicit receiver
                TowerData.TowerLevel(ImportingScopeBasedTowerLevel(this, scope as ImportingScope)).process()?.let { return it }
            }
        }

        return resultCollector.getFinalCandidates()
    }

    private fun MemberScopeTowerLevel.mayFitForName(name: Name) =
            !definitelyDoesNotContainName(name) || !definitelyDoesNotContainName(OperatorNameConventions.INVOKE)

    private fun ResolutionScope.mayFitForName(name: Name) =
            !definitelyDoesNotContainName(name) || !definitelyDoesNotContainName(OperatorNameConventions.INVOKE)

    fun <C : Candidate> runWithEmptyTowerData(
            processor: ScopeTowerProcessor<C>,
            resultCollector: ResultCollector<C>,
            useOrder: Boolean
    ): Collection<C> = processTowerData(processor, resultCollector, useOrder, TowerData.Empty) ?: resultCollector.getFinalCandidates()

    private fun <C : Candidate> processTowerData(
            processor: ScopeTowerProcessor<C>,
            resultCollector: ResultCollector<C>,
            useOrder: Boolean,
            towerData: TowerData
    ): Collection<C>? {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val candidatesGroups = if (useOrder) {
            processor.process(towerData)
        }
        else {
            listOf(processor.process(towerData).flatMap { it })
        }

        for (candidatesGroup in candidatesGroups) {
            resultCollector.pushCandidates(candidatesGroup)
            resultCollector.getSuccessfulCandidates()?.let { return it }
        }

        return null
    }


    abstract class ResultCollector<C : Candidate> {
        abstract fun getSuccessfulCandidates(): Collection<C>?

        abstract fun getFinalCandidates(): Collection<C>

        abstract fun pushCandidates(candidates: Collection<C>)
    }

    class AllCandidatesCollector<C : Candidate>: ResultCollector<C>() {
        private val allCandidates = ArrayList<C>()

        override fun getSuccessfulCandidates(): Collection<C>? = null

        override fun getFinalCandidates(): Collection<C> = allCandidates

        override fun pushCandidates(candidates: Collection<C>) {
            candidates.filterNotTo(allCandidates) {
                it.resultingApplicability == ResolutionCandidateApplicability.HIDDEN
            }
        }
    }

    class SuccessfulResultCollector<C : Candidate> : ResultCollector<C>() {
        private var candidateGroups = arrayListOf<Collection<C>>()
        private var isSuccessful = false

        override fun getSuccessfulCandidates(): Collection<C>? {
            if (!isSuccessful) return null
            val firstGroupWithResolved = candidateGroups.firstOrNull {
                it.any { it.resultingApplicability == ResolutionCandidateApplicability.RESOLVED }
            } ?: return null

            return firstGroupWithResolved.filter { it.resultingApplicability == ResolutionCandidateApplicability.RESOLVED }
        }

        override fun pushCandidates(candidates: Collection<C>) {
            val thereIsSuccessful = candidates.any { it.isSuccessful }
            if (!isSuccessful && !thereIsSuccessful) {
                candidateGroups.add(candidates)
                return
            }

            if (!isSuccessful) {
                candidateGroups.clear()
                isSuccessful = true
            }
            if (thereIsSuccessful) {
                candidateGroups.add(candidates.filter { it.isSuccessful })
            }
        }

        override fun getFinalCandidates(): Collection<C> {
            val moreSuitableGroup = candidateGroups.minBy { it.groupApplicability } ?: return emptyList()
            val groupApplicability = moreSuitableGroup.groupApplicability
            if (groupApplicability == ResolutionCandidateApplicability.HIDDEN) return emptyList()

            return moreSuitableGroup.filter { it.resultingApplicability == groupApplicability }
        }

        private val Collection<C>.groupApplicability get() =
            minBy { it.resultingApplicability }?.resultingApplicability ?: ResolutionCandidateApplicability.HIDDEN
    }
}
