/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.blockGuard
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkContractDescriptionIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isCallableWithSpecialBody
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.isCopyCreatedInScope
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef

internal object LLFirContractsLazyResolver : LLFirLazyResolver(FirResolvePhase.CONTRACTS) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirContractsTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        if (target !is FirContractDescriptionOwner) return
        checkContractDescriptionIsResolved(target)
    }
}

private class LLFirContractsTargetResolver(target: LLFirResolveTarget) : LLFirAbstractBodyTargetResolver(
    target,
    FirResolvePhase.CONTRACTS,
) {
    override val transformer = FirContractResolveTransformer(resolveTargetSession, resolveTargetScopeSession)

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // There is no sense to resolve such declarations as they do not have contracts
        if (target is FirCallableDeclaration && target.isCopyCreatedInScope) return

        when (target) {
            is FirPrimaryConstructor, is FirErrorPrimaryConstructor -> {
                // No contracts here
            }

            is FirSimpleFunction -> {
                // There is no sense to try to transform functions without a block body and without a raw contract
                if (target.returnTypeRef !is FirImplicitTypeRef || target.contractDescription is FirRawContractDescription) {
                    resolve(target, ContractStateKeepers.SIMPLE_FUNCTION)
                }
            }

            is FirConstructor -> resolve(target, ContractStateKeepers.CONSTRUCTOR)
            is FirProperty -> {
                // Property with delegate can't have any contracts
                if (target.delegate == null) {
                    resolve(target, ContractStateKeepers.PROPERTY)
                }
            }
            is FirRegularClass,
            is FirTypeAlias,
            is FirVariable,
            is FirFunction,
            is FirAnonymousInitializer,
            is FirFile,
            is FirScript,
            is FirCodeFragment,
            is FirFileAnnotationsContainer,
            is FirDanglingModifierList,
            -> {
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
    private val CONTRACT_DESCRIPTION_OWNER: StateKeeper<FirContractDescriptionOwner, FirDesignation> = stateKeeper { _, _ ->
        add(FirContractDescriptionOwner::contractDescription, FirContractDescriptionOwner::replaceContractDescription)
    }

    private val BODY_OWNER: StateKeeper<FirFunction, FirDesignation> = stateKeeper { declaration, _ ->
        if (declaration is FirContractDescriptionOwner && declaration.contractDescription is FirRawContractDescription) {
            // No need to change the body, contract is declared separately
            return@stateKeeper
        }

        if (!isCallableWithSpecialBody(declaration)) {
            add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
        }
    }

    val SIMPLE_FUNCTION: StateKeeper<FirSimpleFunction, FirDesignation> = stateKeeper { _, designation ->
        add(CONTRACT_DESCRIPTION_OWNER, designation)
        add(BODY_OWNER, designation)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor, FirDesignation> = stateKeeper { _, designation ->
        add(CONTRACT_DESCRIPTION_OWNER, designation)
        add(BODY_OWNER, designation)
    }

    private val PROPERTY_ACCESSOR: StateKeeper<FirPropertyAccessor, FirDesignation> = stateKeeper { _, designation ->
        add(CONTRACT_DESCRIPTION_OWNER, designation)
        add(BODY_OWNER, designation)
    }

    val PROPERTY: StateKeeper<FirProperty, FirDesignation> = stateKeeper { property, designation ->
        entity(property.getter, PROPERTY_ACCESSOR, designation)
        entity(property.setter, PROPERTY_ACCESSOR, designation)
    }
}