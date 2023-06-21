/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.blockGuard
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isCallableWithSpecialBody
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer

internal object LLFirContractsLazyResolver : LLFirLazyResolver(FirResolvePhase.CONTRACTS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirContractsTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        if (target is FirContractDescriptionOwner) {
            // TODO checkContractDescriptionIsResolved(declaration)
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirContractsTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.CONTRACTS
) {
    override val transformer = FirContractResolveTransformer(session, scopeSession)

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirSimpleFunction -> resolve(target, ContractStateKeepers.SIMPLE_FUNCTION)
            is FirConstructor -> resolve(target, ContractStateKeepers.CONSTRUCTOR)
            is FirProperty -> resolve(target, ContractStateKeepers.PROPERTY)
            is FirPropertyAccessor -> resolve(target, ContractStateKeepers.PROPERTY_ACCESSOR)
            is FirRegularClass,
            is FirTypeAlias,
            is FirVariable,
            is FirFunction,
            is FirAnonymousInitializer,
            is FirScript,
            is FirFileAnnotationsContainer,
            is FirDanglingModifierList -> {
                // No contracts here
                check(target !is FirContractDescriptionOwner) {
                    "Unexpected contract description owner: $target (${target.javaClass.name})"
                }
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}

private object ContractStateKeepers {
    private val CONTRACT_DESCRIPTION_OWNER: StateKeeper<FirContractDescriptionOwner> = stateKeeper {
        add(FirContractDescriptionOwner::contractDescription, FirContractDescriptionOwner::replaceContractDescription)
    }

    private val BODY_OWNER: StateKeeper<FirFunction> = stateKeeper { declaration ->
        if (declaration is FirContractDescriptionOwner && declaration.contractDescription is FirRawContractDescription) {
            // No need to change the body, contract is declared separately
            return@stateKeeper
        }

        if (!isCallableWithSpecialBody(declaration)) {
            add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
        }
    }

    val SIMPLE_FUNCTION: StateKeeper<FirSimpleFunction> = stateKeeper {
        add(CONTRACT_DESCRIPTION_OWNER)
        add(BODY_OWNER)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor> = stateKeeper {
        add(CONTRACT_DESCRIPTION_OWNER)
        add(BODY_OWNER)
    }

    val PROPERTY_ACCESSOR: StateKeeper<FirPropertyAccessor> = stateKeeper {
        add(CONTRACT_DESCRIPTION_OWNER)
        add(BODY_OWNER)
    }

    val PROPERTY: StateKeeper<FirProperty> = stateKeeper { property ->
        entity(property.getter, PROPERTY_ACCESSOR)
        entity(property.setter, PROPERTY_ACCESSOR)
    }
}