/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

internal class CandidateFactoriesAndCollectors(
    // Common calls
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector,
)


internal class TowerLevelHandler {

    // Try to avoid adding additional state here
    private var processResult = ProcessResult.SCOPE_EMPTY

    fun handleLevel(
        collector: CandidateCollector,
        candidateFactory: CandidateFactory,
        info: CallInfo,
        explicitReceiverKind: ExplicitReceiverKind,
        group: TowerGroup,
        towerLevel: SessionBasedTowerLevel
    ): ProcessResult {
        processResult = ProcessResult.SCOPE_EMPTY
        val processor =
            TowerScopeLevelProcessor(
                info,
                explicitReceiverKind,
                collector,
                candidateFactory,
                group
            )

        when (info.callKind) {
            CallKind.VariableAccess -> {
                processResult += towerLevel.processPropertiesByName(info, processor)

                if (!collector.isSuccess() && towerLevel is ScopeTowerLevel && towerLevel.extensionReceiver == null) {
                    processResult += towerLevel.processObjectsByName(info, processor)
                }
            }
            CallKind.Function -> {
                processResult += towerLevel.processFunctionsByName(info, processor)
            }
            CallKind.CallableReference -> {
                processResult += towerLevel.processFunctionsByName(info, processor)
                processResult += towerLevel.processPropertiesByName(info, processor)
            }
            else -> {
                throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
            }
        }
        return processResult
    }
}

private class TowerScopeLevelProcessor(
    val callInfo: CallInfo,
    val explicitReceiverKind: ExplicitReceiverKind,
    val resultCollector: CandidateCollector,
    val candidateFactory: CandidateFactory,
    val group: TowerGroup
) : TowerScopeLevel.TowerScopeLevelProcessor<FirBasedSymbol<*>> {
    override fun consumeCandidate(
        symbol: FirBasedSymbol<*>,
        dispatchReceiverValue: ReceiverValue?,
        extensionReceiverValue: ReceiverValue?,
        scope: FirScope,
        builtInExtensionFunctionReceiverValue: ReceiverValue?
    ) {
        // If we are about to consume a property
        // candidate where the property has an
        // exposing getter (the one with a more
        // permissive visibility), we should prepare
        // a separate candidate for that getter

        // This candidate will be checked by
        // VisibilityChecker after the main one
        // and will be disabled if the property
        // can be accessed directly

        var dependentGetterCandidate: Candidate? = null

        if (symbol is FirPropertySymbol) {
            dependentGetterCandidate = consumePropertyGetterIfNeeded(
                symbol.fir,
                dispatchReceiverValue,
                extensionReceiverValue,
                scope,
                builtInExtensionFunctionReceiverValue
            )
        }

        resultCollector.consumeCandidate(
            group,
            candidateFactory.createCandidate(
                callInfo,
                symbol,
                explicitReceiverKind,
                scope,
                dispatchReceiverValue,
                extensionReceiverValue,
                builtInExtensionFunctionReceiverValue
            ).apply {
                possiblyInvisibleDependentCandidate = dependentGetterCandidate
            },
            candidateFactory.context
        )

        // It's important to collect the getter
        // after its property so that VisibilityChecker
        // is able to check the property visibility first
        // and disable the getter
        dependentGetterCandidate?.let {
            resultCollector.consumeCandidate(group, it, candidateFactory.context)
        }
    }

    private fun consumePropertyGetterIfNeeded(
        property: FirProperty,
        dispatchReceiverValue: ReceiverValue?,
        extensionReceiverValue: ReceiverValue?,
        scope: FirScope,
        builtInExtensionFunctionReceiverValue: ReceiverValue?
    ): Candidate? {
        val getter = property.getter ?: return null

        // Right now a getter can't have
        // a visibility smaller than the
        // property
        if (getter.visibility == property.visibility) {
            return null
        }

        // So here the getter has a greater
        // visibility
        return candidateFactory.createCandidate(
            callInfo,
            getter.symbol,
            explicitReceiverKind,
            scope,
            dispatchReceiverValue,
            extensionReceiverValue,
            builtInExtensionFunctionReceiverValue
        )
    }

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }
}
