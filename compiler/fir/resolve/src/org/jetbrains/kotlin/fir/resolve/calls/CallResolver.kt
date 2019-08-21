/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope

enum class TowerDataKind {
    EMPTY,
    TOWER_LEVEL
}

class CallResolver(
    val typeCalculator: ReturnTypeCalculator,
    val components: InferenceComponents,
    val resolutionStageRunner: ResolutionStageRunner,
    val topLevelScopes: List<FirScope>,
    val localScopes: List<FirLocalScope>
) {

    val session: FirSession get() = components.session

    private fun processImplicitReceiver(
        towerDataConsumer: TowerDataConsumer,
        implicitReceiverValue: ImplicitReceiverValue,
        collector: CandidateCollector,
        oldGroup: Int
    ): Int {
        var group = oldGroup
        towerDataConsumer.consume(
            TowerDataKind.TOWER_LEVEL,
            MemberScopeTowerLevel(session, implicitReceiverValue, scopeSession = components.scopeSession),
            group++
        )

        // This is an equivalent to the old "BothTowerLevelAndImplicitReceiver"
        towerDataConsumer.consume(
            TowerDataKind.TOWER_LEVEL,
            MemberScopeTowerLevel(session, implicitReceiverValue, implicitReceiverValue, components.scopeSession),
            group++
        )

        for (scope in localScopes) {
            towerDataConsumer.consume(
                TowerDataKind.TOWER_LEVEL,
                ScopeTowerLevel(session, scope, implicitExtensionReceiver = implicitReceiverValue),
                group++
            )
        }

        for (implicitDispatchReceiverValue in implicitReceiverValues) {
            val implicitScope = implicitDispatchReceiverValue.implicitScope
            if (implicitScope != null) {
                towerDataConsumer.consume(
                    TowerDataKind.TOWER_LEVEL,
                    ScopeTowerLevel(session, implicitScope, implicitExtensionReceiver = implicitReceiverValue),
                    group++
                )
            }
            if (implicitDispatchReceiverValue is ImplicitDispatchReceiverValue) {
                val implicitCompanionScope = implicitDispatchReceiverValue.implicitCompanionScope
                if (implicitCompanionScope != null) {
                    towerDataConsumer.consume(
                        TowerDataKind.TOWER_LEVEL,
                        ScopeTowerLevel(session, implicitCompanionScope, implicitExtensionReceiver = implicitReceiverValue),
                        group++
                    )
                }
            }
            if (implicitDispatchReceiverValue !== implicitReceiverValue) {
                // Two different implicit receivers
                towerDataConsumer.consume(
                    TowerDataKind.TOWER_LEVEL,
                    MemberScopeTowerLevel(
                        session, scopeSession = components.scopeSession,
                        dispatchReceiver = implicitDispatchReceiverValue,
                        implicitExtensionReceiver = implicitReceiverValue
                    ),
                    group++
                )
            }
        }

        for (scope in topLevelScopes) {
            towerDataConsumer.consume(
                TowerDataKind.TOWER_LEVEL,
                ScopeTowerLevel(session, scope, implicitExtensionReceiver = implicitReceiverValue),
                group++
            )
        }

        return group
    }

    val collector by lazy { CandidateCollector(components, resolutionStageRunner) }
    lateinit var towerDataConsumer: TowerDataConsumer
    private lateinit var implicitReceiverValues: List<ImplicitReceiverValue>

    fun runTowerResolver(consumer: TowerDataConsumer, implicitReceiverValues: List<ImplicitReceiverValue>): CandidateCollector {
        this.implicitReceiverValues = implicitReceiverValues
        towerDataConsumer = consumer

        var group = 0
        // Member of explicit receiver' type
        towerDataConsumer.consume(TowerDataKind.EMPTY, TowerScopeLevel.Empty, group++)

        // Member of local scope
        for (scope in localScopes) {
            towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, ScopeTowerLevel(session, scope), group++)
        }

        var blockDispatchReceivers = false

        // Member of implicit receiver' type
        for (implicitReceiverValue in implicitReceiverValues) {
            val implicitScope = implicitReceiverValue.implicitScope
            if (implicitScope != null) {
                towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, ScopeTowerLevel(session, implicitScope), group++)
            }
            if (implicitReceiverValue is ImplicitDispatchReceiverValue) {
                val implicitCompanionScope = implicitReceiverValue.implicitCompanionScope
                if (implicitCompanionScope != null) {
                    towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, ScopeTowerLevel(session, implicitCompanionScope), group++)
                }
                if (blockDispatchReceivers) {
                    continue
                }
                if (!implicitReceiverValue.boundSymbol.fir.isInner) {
                    blockDispatchReceivers = true
                }
            }
            group = processImplicitReceiver(towerDataConsumer, implicitReceiverValue, collector, group)
        }

        // Member of top-level scope
        for (scope in topLevelScopes) {
            towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, ScopeTowerLevel(session, scope), group++)
        }

        return collector
    }
}