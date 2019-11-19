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
import org.jetbrains.kotlin.fir.types.ConeClassType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker

abstract class TowerDataConsumer {
    abstract fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
//        resultCollector: CandidateCollector,
        group: Int
    ): ProcessorAction

    private var stopGroup = Int.MAX_VALUE
    fun skipGroup(group: Int, resultCollector: CandidateCollector): Boolean {
        if (resultCollector.isSuccess() && stopGroup == Int.MAX_VALUE) {
            stopGroup = group
        }
        if (group >= stopGroup) return true
        return false
    }
}

class QualifiedReceiverTowerDataConsumer<T : AbstractFirBasedSymbol<*>>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val explicitReceiver: ExpressionReceiverValue,
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector
) : TowerDataConsumer() {
    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        if (kind != TowerDataKind.EMPTY) return ProcessorAction.NEXT

        return QualifiedReceiverTowerLevel(session, candidateFactory.bodyResolveComponents).processElementsByName(
            token,
            name,
            explicitReceiver,
            processor = TowerLevelProcessorImpl(group)
        )
    }

    private inner class TowerLevelProcessorImpl(val group: Int) : TowerScopeLevel.TowerScopeLevelProcessor<T> {
        override fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ClassDispatchReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?
        ): ProcessorAction {
            assert(dispatchReceiverValue == null)
            resultCollector.consumeCandidate(
                group,
                candidateFactory.createCandidate(
                    symbol,
                    dispatchReceiverValue = null,
                    implicitExtensionReceiverValue = null,
                    explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                )
            )
            return ProcessorAction.NEXT
        }
    }

}

class PrioritizedTowerDataConsumer(
    val resultCollector: CandidateCollector,
    vararg val consumers: TowerDataConsumer
) : TowerDataConsumer() {
    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        var empty = true
        for ((index, consumer) in consumers.withIndex()) {
            val action = consumer.consume(kind, towerScopeLevel, group * consumers.size + index)
            when (action) {
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
    private val resultCollector: CandidateCollector
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
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
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

    fun addConsumer(consumer: TowerDataConsumer): ProcessorAction {
        additionalConsumers += consumer
        for ((kind, level, group) in accumulatedTowerData) {
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
    val explicitReceiver: ExpressionReceiverValue,
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector
) : TowerDataConsumer() {

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }

    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        return when (kind) {
            TowerDataKind.EMPTY ->
                MemberScopeTowerLevel(
                    session, resultCollector.components, explicitReceiver, scopeSession = candidateFactory.bodyResolveComponents.scopeSession
                ).processElementsByName(
                    token,
                    name,
                    explicitReceiver = null,
                    processor = EmptyKindTowerProcessor(group)
                )
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
            dispatchReceiverValue: ClassDispatchReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?
        ): ProcessorAction {
            resultCollector.consumeCandidate(
                group,
                candidateFactory.createCandidate(
                    symbol,
                    dispatchReceiverValue,
                    implicitExtensionReceiverValue,
                    ExplicitReceiverKind.DISPATCH_RECEIVER
                )
            )
            return ProcessorAction.NEXT
        }
    }

    private inner class TowerLevelKindTowerProcessor(val group: Int) : TowerScopeLevel.TowerScopeLevelProcessor<T> {
        override fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ClassDispatchReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?
        ): ProcessorAction {
            if (symbol is FirNamedFunctionSymbol && symbol.callableId.packageName.startsWith(defaultPackage)) {
                val explicitReceiverType = explicitReceiver.type
                if (dispatchReceiverValue == null && explicitReceiverType is ConeClassType) {
                    val declarationReceiverTypeRef =
                        (symbol as? FirCallableSymbol<*>)?.fir?.receiverTypeRef as? FirResolvedTypeRef
                    val declarationReceiverType = declarationReceiverTypeRef?.type
                    if (declarationReceiverType is ConeClassType) {
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
                dispatchReceiverValue,
                implicitExtensionReceiverValue,
                ExplicitReceiverKind.EXTENSION_RECEIVER
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
    val resultCollector: CandidateCollector
) : TowerDataConsumer() {
    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
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
            dispatchReceiverValue: ClassDispatchReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue<*>?
        ): ProcessorAction {
            resultCollector.consumeCandidate(
                group,
                candidateFactory.createCandidate(
                    symbol,
                    dispatchReceiverValue,
                    implicitExtensionReceiverValue,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                )
            )
            return ProcessorAction.NEXT
        }
    }
}
