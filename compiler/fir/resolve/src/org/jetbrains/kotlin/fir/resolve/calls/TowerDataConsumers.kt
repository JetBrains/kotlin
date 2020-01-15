/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.constructClassType
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker

abstract class TowerDataConsumer {
    abstract val resultCollector: CandidateCollector

    abstract fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction

    private var stopGroup = Int.MAX_VALUE

    fun skipGroup(group: Int): Boolean {
        if (stopGroup == Int.MAX_VALUE && resultCollector.isSuccess()) {
            stopGroup = group
        }
        if (group >= stopGroup) return true
        return false
    }
}

class PrioritizedTowerDataConsumer(
    override val resultCollector: CandidateCollector,
    private vararg val consumers: TowerDataConsumer
) : TowerDataConsumer() {
    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group)) return ProcessorAction.NEXT
        var empty = true
        for ((index, consumer) in consumers.withIndex()) {
            when (val action = consumer.consume(kind, towerScopeLevel, group * consumers.size + index)) {
                ProcessorAction.STOP -> return action
                ProcessorAction.NEXT -> empty = false
            }
        }
        return if (empty) ProcessorAction.NONE else ProcessorAction.NEXT
    }
}

// Yet is used exclusively for invokes:
// - initialConsumer consumes property which is invoke receiver
// - additionalConsumers consume invoke calls themselves
class AccumulatingTowerDataConsumer(
    override val resultCollector: CandidateCollector
) : TowerDataConsumer() {
    lateinit var initialConsumer: TowerDataConsumer

    private val additionalConsumers = mutableListOf<TowerDataConsumer>()

    private data class TowerData(val kind: TowerDataKind, val level: TowerScopeLevel, val group: Int)

    private val accumulatedTowerData = mutableListOf<TowerData>()

    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group)) return ProcessorAction.NEXT
        accumulatedTowerData += TowerData(kind, towerScopeLevel, group)

        var empty = true
        when (val action = initialConsumer.consume(kind, towerScopeLevel, group)) {
            ProcessorAction.STOP -> return action
            ProcessorAction.NEXT -> empty = false
        }
        for (consumer in additionalConsumers) {
            when (val action = consumer.consume(kind, towerScopeLevel, group)) {
                ProcessorAction.STOP -> return action
                ProcessorAction.NEXT -> empty = false
            }
        }
        return if (empty) ProcessorAction.NONE else ProcessorAction.NEXT
    }

    fun addConsumerAndProcessAccumulatedData(consumer: TowerDataConsumer): ProcessorAction {
        additionalConsumers += consumer
        if (accumulatedTowerData.isEmpty()) return ProcessorAction.NEXT
        for ((kind, level, group) in accumulatedTowerData.asSequence().take(accumulatedTowerData.size - 1)) {
            if (consumer.consume(kind, level, group).stop()) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}

class ExplicitReceiverTowerDataConsumer<T : AbstractFirBasedSymbol<*>>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val explicitReceiver: AbstractExplicitReceiver<*>,
    val candidateFactory: CandidateFactory,
    override val resultCollector: CandidateCollector
) : TowerDataConsumer() {

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }

    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group)) return ProcessorAction.NEXT
        return when (kind) {
            TowerDataKind.EMPTY -> {
                MemberScopeTowerLevel(
                    session, resultCollector.components, explicitReceiver,
                    implicitExtensionReceiver = (towerScopeLevel as? TowerScopeLevel.OnlyImplicitReceiver)?.implicitReceiverValue,
                    implicitExtensionInvokeMode = towerScopeLevel is TowerScopeLevel.OnlyImplicitReceiver,
                    scopeSession = candidateFactory.bodyResolveComponents.scopeSession
                ).processElementsByName(
                    token,
                    name,
                    explicitReceiver = null,
                    processor = EmptyKindTowerProcessor(group)
                )
            }
            TowerDataKind.TOWER_LEVEL -> {
                if (token == TowerScopeLevel.Token.Objects) return ProcessorAction.NEXT
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    explicitReceiver = explicitReceiver,
                    processor = TowerLevelKindTowerProcessor(group)
                )
            }
        }
    }


    private inner class EmptyKindTowerProcessor(val group: Int) : TowerScopeLevel.TowerScopeLevelProcessor<T> {
        override fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
            builtInExtensionFunctionReceiverValue: ReceiverValue?
        ): ProcessorAction {
            resultCollector.consumeCandidate(
                group,
                candidateFactory.createCandidate(
                    symbol,
                    ExplicitReceiverKind.DISPATCH_RECEIVER,
                    dispatchReceiverValue,
                    implicitExtensionReceiverValue,
                    builtInExtensionFunctionReceiverValue
                )
            )
            return ProcessorAction.NEXT
        }
    }

    private inner class TowerLevelKindTowerProcessor(val group: Int) : TowerScopeLevel.TowerScopeLevelProcessor<T> {
        override fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
            builtInExtensionFunctionReceiverValue: ReceiverValue?
        ): ProcessorAction {
            if (symbol is FirNamedFunctionSymbol && symbol.callableId.packageName.startsWith(defaultPackage)) {
                val explicitReceiverType = explicitReceiver.type
                if (dispatchReceiverValue == null && explicitReceiverType is ConeClassLikeType) {
                    val declarationReceiverTypeRef =
                        (symbol as? FirCallableSymbol<*>)?.fir?.receiverTypeRef as? FirResolvedTypeRef
                    val declarationReceiverType = declarationReceiverTypeRef?.type
                    if (declarationReceiverType is ConeClassLikeType) {
                        if (!AbstractTypeChecker.isSubtypeOf(
                                candidateFactory.bodyResolveComponents.inferenceComponents.ctx,
                                explicitReceiverType,
                                declarationReceiverType.lookupTag.constructClassType(
                                    declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
                                    isNullable = true
                                )
                            )
                        ) {
                            return ProcessorAction.NEXT
                        }
                    }
                }
            }
            val candidate = candidateFactory.createCandidate(
                symbol,
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                dispatchReceiverValue,
                implicitExtensionReceiverValue,
                builtInExtensionFunctionReceiverValue
            )

            resultCollector.consumeCandidate(
                group,
                candidate
            )
            return ProcessorAction.NEXT
        }
    }
}

class NoExplicitReceiverTowerDataConsumer<T : AbstractFirBasedSymbol<*>>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val candidateFactory: CandidateFactory,
    override val resultCollector: CandidateCollector
) : TowerDataConsumer() {
    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group)) return ProcessorAction.NEXT
        return when (kind) {

            TowerDataKind.TOWER_LEVEL -> {
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    explicitReceiver = null,
                    processor = TowerLevelProcessorImpl(group)
                )
            }
            else -> ProcessorAction.NEXT
        }
    }

    private inner class TowerLevelProcessorImpl(val group: Int) : TowerScopeLevel.TowerScopeLevelProcessor<T> {
        override fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
            builtInExtensionFunctionReceiverValue: ReceiverValue?
        ): ProcessorAction {
            resultCollector.consumeCandidate(
                group,
                candidateFactory.createCandidate(
                    symbol,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    dispatchReceiverValue,
                    implicitExtensionReceiverValue,
                    builtInExtensionFunctionReceiverValue
                )
            )
            return ProcessorAction.NEXT
        }
    }
}
