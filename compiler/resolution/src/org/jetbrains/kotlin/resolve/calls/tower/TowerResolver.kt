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

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

interface Candidate {
    // this operation should be very fast
    val isSuccessful: Boolean

    val resultingApplicability: ResolutionCandidateApplicability

    fun addCompatibilityWarning(other: Candidate)
}

interface CandidateFactory<out C : Candidate> {
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
    class OnlyImplicitReceiver(val implicitReceiver: ReceiverValueWithSmartCastInfo) : TowerData()
    class TowerLevel(val level: ScopeTowerLevel) : TowerData()
    class BothTowerLevelAndImplicitReceiver(val level: ScopeTowerLevel, val implicitReceiver: ReceiverValueWithSmartCastInfo) : TowerData()
    // Has the same meaning as BothTowerLevelAndImplicitReceiver, but it's only used for names lookup, so it doesn't need implicit receiver
    class ForLookupForNoExplicitReceiver(val level: ScopeTowerLevel) : TowerData()
}

interface ScopeTowerProcessor<out C> {
    // Candidates with matched receivers (dispatch receiver was already matched in ScopeTowerLevel)
    // Candidates in one groups have same priority, first group has highest priority.
    fun process(data: TowerData): List<Collection<C>>

    fun recordLookups(skippedData: Collection<TowerData>, name: Name)
}

class TowerResolver {
    fun <C : Candidate> runResolve(
        scopeTower: ImplicitScopeTower,
        processor: ScopeTowerProcessor<C>,
        useOrder: Boolean,
        name: Name
    ): Collection<C> = scopeTower.run(processor, SuccessfulResultCollector(), useOrder, name)

    fun <C : Candidate> collectAllCandidates(
        scopeTower: ImplicitScopeTower,
        processor: ScopeTowerProcessor<C>,
        name: Name
    ): Collection<C> = scopeTower.run(processor, AllCandidatesCollector(), false, name)

    private fun <C : Candidate> ImplicitScopeTower.run(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean,
        name: Name
    ): Collection<C> = Task(this, processor, resultCollector, useOrder, name).run()

    private inner class Task<out C : Candidate>(
        private val implicitScopeTower: ImplicitScopeTower,
        private val processor: ScopeTowerProcessor<C>,
        private val resultCollector: ResultCollector<C>,
        private val useOrder: Boolean,
        private val name: Name
    ) {
        private val isNameForHidesMember = name in HIDES_MEMBERS_NAME_LIST
        private val skippedDataForLookup = mutableListOf<TowerData>()

        private val localLevels: Collection<ScopeTowerLevel> by lazy(LazyThreadSafetyMode.NONE) {
            implicitScopeTower.lexicalScope.parentsWithSelf.filterIsInstance<LexicalScope>()
                .filter { it.kind.withLocalDescriptors && it.mayFitForName(name) }.map { ScopeBasedTowerLevel(implicitScopeTower, it) }
                .toList()
        }

        private val nonLocalLevels: Collection<ScopeTowerLevel> by lazy(LazyThreadSafetyMode.NONE) {
            implicitScopeTower.createNonLocalLevels()
        }

        val hidesMembersLevel = HidesMembersTowerLevel(implicitScopeTower)
        val syntheticLevel = SyntheticScopeBasedTowerLevel(implicitScopeTower, implicitScopeTower.syntheticScopes)

        private fun ImplicitScopeTower.createNonLocalLevels(): Collection<ScopeTowerLevel> {
            val mainResult = mutableListOf<ScopeTowerLevel>()

            fun addLevel(scopeTowerLevel: ScopeTowerLevel, mayFitForName: Boolean) {
                if (mayFitForName) {
                    mainResult.add(scopeTowerLevel)
                } else {
                    skippedDataForLookup.add(TowerData.ForLookupForNoExplicitReceiver(scopeTowerLevel))
                }
            }

            lexicalScope.parentsWithSelf.forEach { scope ->
                if (scope is LexicalScope) {
                    if (!scope.kind.withLocalDescriptors) {
                        addLevel(
                            ScopeBasedTowerLevel(this@createNonLocalLevels, scope),
                            scope.mayFitForName(name)
                        )
                    }

                    getImplicitReceiver(scope)?.let {
                        addLevel(
                            MemberScopeTowerLevel(this@createNonLocalLevels, it),
                            it.mayFitForName(name)
                        )
                    }
                } else {
                    addLevel(
                        ImportingScopeBasedTowerLevel(this@createNonLocalLevels, scope as ImportingScope),
                        scope.mayFitForName(name)
                    )
                }
            }

            return mainResult
        }

        private fun TowerData.process() = processTowerData(processor, resultCollector, useOrder, this)?.also {
            recordLookups()
        }

        private fun TowerData.process(mayFitForName: Boolean): Collection<C>? {
            if (!mayFitForName) {
                skippedDataForLookup.add(this)
                return null
            }
            return process()
        }

        fun run(): Collection<C> {
            if (isNameForHidesMember) {
                // hides members extensions for explicit receiver
                TowerData.TowerLevel(hidesMembersLevel).process()?.let { return it }
            }

            // possibly there is explicit member
            TowerData.Empty.process()?.let { return it }
            // synthetic property for explicit receiver
            TowerData.TowerLevel(syntheticLevel).process()?.let { return it }

            // local non-extensions or extension for explicit receiver
            for (localLevel in localLevels) {
                TowerData.TowerLevel(localLevel).process()?.let { return it }
            }

            for (scopeInfo in implicitScopeTower.allScopesWithImplicitsResolutionInfo()) {
                val scope = scopeInfo.scope
                if (scope is LexicalScope) {
                    // statics
                    if (!scope.kind.withLocalDescriptors) {
                        TowerData.TowerLevel(ScopeBasedTowerLevel(implicitScopeTower, scope))
                            .process(scope.mayFitForName(name))?.let { return it }
                    }

                    implicitScopeTower.getImplicitReceiver(scope)
                        ?.let { processImplicitReceiver(it, scopeInfo.resolveExtensionsForImplicitReceiver) }
                        ?.let { return it }
                } else {
                    TowerData.TowerLevel(ImportingScopeBasedTowerLevel(implicitScopeTower, scope as ImportingScope))
                        .process(scope.mayFitForName(name))?.let { return it }
                }
            }

            recordLookups()

            return resultCollector.getFinalCandidates()
        }

        private fun processImplicitReceiver(implicitReceiver: ReceiverValueWithSmartCastInfo, resolveExtensions: Boolean): Collection<C>? {
            if (isNameForHidesMember) {
                // hides members extensions
                TowerData.BothTowerLevelAndImplicitReceiver(hidesMembersLevel, implicitReceiver).process()?.let { return it }
            }

            // members of implicit receiver or member extension for explicit receiver
            TowerData.TowerLevel(MemberScopeTowerLevel(implicitScopeTower, implicitReceiver))
                .process(implicitReceiver.mayFitForName(name))?.let { return it }

            // synthetic properties
            TowerData.BothTowerLevelAndImplicitReceiver(syntheticLevel, implicitReceiver).process()?.let { return it }

            if (resolveExtensions) {
                // invokeExtension on local variable
                TowerData.OnlyImplicitReceiver(implicitReceiver).process()?.let { return it }

                // local extensions for implicit receiver
                for (localLevel in localLevels) {
                    TowerData.BothTowerLevelAndImplicitReceiver(localLevel, implicitReceiver).process()?.let { return it }
                }

                // extension for implicit receiver
                for (nonLocalLevel in nonLocalLevels) {
                    TowerData.BothTowerLevelAndImplicitReceiver(nonLocalLevel, implicitReceiver).process()?.let { return it }
                }
            }

            return null
        }

        private fun recordLookups() {
            processor.recordLookups(skippedDataForLookup, name)
        }

        private fun ReceiverValueWithSmartCastInfo.mayFitForName(name: Name): Boolean {
            if (receiverValue.type.mayFitForName(name)) return true
            if (!hasTypesFromSmartCasts()) return false
            return typesFromSmartCasts.any { it.mayFitForName(name) }
        }

        private fun KotlinType.mayFitForName(name: Name) =
            isDynamic() ||
                    !memberScope.definitelyDoesNotContainName(name) ||
                    !memberScope.definitelyDoesNotContainName(OperatorNameConventions.INVOKE)

        private fun ResolutionScope.mayFitForName(name: Name) =
            !definitelyDoesNotContainName(name) || !definitelyDoesNotContainName(OperatorNameConventions.INVOKE)
    }

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
        } else {
            listOf(processor.process(towerData).flatten())
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

    class AllCandidatesCollector<C : Candidate> : ResultCollector<C>() {
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
            var compatibilityCandidate: C? = null
            var compatibilityGroup: Collection<C>? = null
            var firstGroupWithResolved: Collection<C>? = null
            outer@ for (group in candidateGroups) {
                for (candidate in group) {
                    if (isSuccessfulCandidate(candidate)) {
                        firstGroupWithResolved = group
                        break@outer
                    }

                    if (compatibilityCandidate == null && isSuccessfulPreserveCompatibility(candidate)) {
                        compatibilityGroup = group
                        compatibilityCandidate = candidate
                    }
                }
            }

            if (firstGroupWithResolved == null) return null
            if (compatibilityCandidate != null && compatibilityGroup !== firstGroupWithResolved) {
                firstGroupWithResolved.forEach { it.addCompatibilityWarning(compatibilityCandidate) }
            }

            return firstGroupWithResolved.filter(::isSuccessfulCandidate)
        }

        private fun isSuccessfulCandidate(candidate: C): Boolean {
            return candidate.resultingApplicability == ResolutionCandidateApplicability.RESOLVED
                    || candidate.resultingApplicability == ResolutionCandidateApplicability.RESOLVED_WITH_ERROR
        }

        private fun isSuccessfulPreserveCompatibility(candidate: C): Boolean =
            candidate.resultingApplicability == ResolutionCandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY

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

        private val Collection<C>.groupApplicability
            get() =
                minBy { it.resultingApplicability }?.resultingApplicability ?: ResolutionCandidateApplicability.HIDDEN
    }
}
