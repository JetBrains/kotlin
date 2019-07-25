/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope

enum class TowerDataKind {
    EMPTY,
    TOWER_LEVEL
}

class CallResolver(val typeCalculator: ReturnTypeCalculator, val components: InferenceComponents) {
    var callInfo: CallInfo? = null
    var scopes: List<FirScope>? = null

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

        for (scope in scopes!!) {
            towerDataConsumer.consume(
                TowerDataKind.TOWER_LEVEL,
                ScopeTowerLevel(session, scope, implicitReceiverValue),
                group++
            )
        }

        return group
    }

    val collector by lazy { CandidateCollector(components) }
    lateinit var towerDataConsumer: TowerDataConsumer
    private lateinit var implicitReceiverValues: List<ImplicitReceiverValue>

    fun runTowerResolver(consumer: TowerDataConsumer, implicitReceiverValues: List<ImplicitReceiverValue>): CandidateCollector {
        this.implicitReceiverValues = implicitReceiverValues
        towerDataConsumer = consumer

        var group = 0
        towerDataConsumer.consume(TowerDataKind.EMPTY, TowerScopeLevel.Empty, group++)

        for (scope in scopes!!) {
            towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, ScopeTowerLevel(session, scope), group++)
        }

        var blockDispatchReceivers = false
        for (implicitReceiverValue in implicitReceiverValues) {
            if (implicitReceiverValue is ImplicitDispatchReceiverValue) {
                if (blockDispatchReceivers) {
                    continue
                }
                if (!implicitReceiverValue.boundSymbol.fir.isInner) {
                    blockDispatchReceivers = true
                }
            }
            group = processImplicitReceiver(towerDataConsumer, implicitReceiverValue, collector, group)
        }

        return collector
    }
}