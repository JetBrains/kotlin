/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.blockGuard
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkContractDescriptionIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.contractDescriptionGuard
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isCallableWithSpecialBody
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.canHaveDeferredReturnTypeCalculation
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef

internal object LLFirContractsLazyResolver : LLFirLazyResolver(FirResolvePhase.CONTRACTS) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirContractsTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        if (target !is FirContractDescriptionOwner) return
        checkContractDescriptionIsResolved(target)
    }
}

/**
 * This resolver is responsible for [CONTRACTS][FirResolvePhase.CONTRACTS] phase.
 *
 * This resolver:
 * - Transforms a [contract][org.jetbrains.kotlin.fir.contracts.FirContractDescription]
 *   definition in [contract owners][FirContractDescriptionOwner].
 *
 * Before the transformation, the resolver [recreates][ContractStateKeepers] bodies
 * to prevent corrupted states due to [PCE][com.intellij.openapi.progress.ProcessCanceledException].
 *
 * @see ContractStateKeepers
 * @see FirContractResolveTransformer
 * @see FirResolvePhase.CONTRACTS
 */
private class LLFirContractsTargetResolver(target: LLFirResolveTarget) : LLFirAbstractBodyTargetResolver(
    target,
    FirResolvePhase.CONTRACTS,
) {
    override val transformer = FirContractResolveTransformer(resolveTargetSession, resolveTargetScopeSession)

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // There is no sense to resolve such declarations as they do not have contracts
        if (target is FirCallableDeclaration && target.canHaveDeferredReturnTypeCalculation) return

        when (target) {
            is FirPrimaryConstructor, is FirErrorPrimaryConstructor -> {
                // No contracts here
            }

            is FirSimpleFunction -> {
                // There is no sense to try to transform functions without a block body and without a raw contract
                if (target.returnTypeRef !is FirImplicitTypeRef || target.contractDescription is FirRawContractDescription) {
                    resolveContracts(target, ContractStateKeepers.SIMPLE_FUNCTION)
                }
            }

            is FirConstructor -> resolveContracts(target, ContractStateKeepers.CONSTRUCTOR)
            is FirProperty -> {
                // Property with delegate can't have any contracts
                if (target.delegate == null) {
                    resolveContracts(target, ContractStateKeepers.PROPERTY)
                }
            }

            is FirRegularClass,
            is FirTypeAlias,
            is FirVariable,
            is FirFunction,
            is FirAnonymousInitializer,
            is FirFile,
            is FirScript,
            is FirReplSnippet,
            is FirCodeFragment,
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

    private fun <T> resolveContracts(
        target: T,
        keeper: StateKeeper<T, FirDesignation>,
    ) where T : FirElementWithResolveState {
        val firDesignation = FirDesignation(containingDeclarations, target)
        resolveWithKeeper(target, firDesignation, keeper, { FirLazyBodiesCalculator.calculateContracts(firDesignation) }) {
            rawResolve(target)
            dropRedundantContractDescription(target)
        }
    }

    private fun dropRedundantContractDescription(target: FirElementWithResolveState) {
        when (target) {
            is FirSimpleFunction, is FirConstructor -> dropRedundantContractDescriptionForFunction(target)
            is FirProperty -> {
                target.getter?.let(::dropRedundantContractDescriptionForFunction)
                target.setter?.let(::dropRedundantContractDescriptionForFunction)
            }
        }
    }

    /**
     * For [org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription] there is no way to understand if we have it or not
     * without calculation a body.
     * And even after that we still may have a case when the transformer can realize after resolution that `contract` call is not a
     * contract, but some another call.
     *
     * In this case we can safely drop the calculated body as it is unnecessary and anyway will be recreated on next phases.
     */
    private fun <T> dropRedundantContractDescriptionForFunction(target: T) where T : FirFunction, T : FirContractDescriptionOwner {
        val contractDescription = target.contractDescription
        // Declarations with initial [FirRawContractDescription] doesn't require to drop body as they don't calculate it
        if (contractDescription is FirResolvedContractDescription) return

        if (target.body != null && !isCallableWithSpecialBody(target)) {
            target.replaceBody(buildLazyBlock())
        }
    }
}

private object ContractStateKeepers {
    private val CONTRACT_DESCRIPTION_OWNER: StateKeeper<FirContractDescriptionOwner, FirDesignation> = stateKeeper { builder, _, _ ->
        builder.add(
            FirContractDescriptionOwner::contractDescription,
            FirContractDescriptionOwner::replaceContractDescription,
            ::contractDescriptionGuard,
        )
    }

    private val BODY_OWNER: StateKeeper<FirFunction, FirDesignation> = stateKeeper { builder, declaration, _ ->
        if (declaration is FirContractDescriptionOwner && declaration.contractDescription is FirRawContractDescription) {
            // No need to change the body, contract is declared separately
            return@stateKeeper
        }

        if (!isCallableWithSpecialBody(declaration)) {
            builder.add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
        }
    }

    val SIMPLE_FUNCTION: StateKeeper<FirSimpleFunction, FirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(CONTRACT_DESCRIPTION_OWNER, designation)
        builder.add(BODY_OWNER, designation)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor, FirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(CONTRACT_DESCRIPTION_OWNER, designation)
        builder.add(BODY_OWNER, designation)
    }

    private val PROPERTY_ACCESSOR: StateKeeper<FirPropertyAccessor, FirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(CONTRACT_DESCRIPTION_OWNER, designation)
        builder.add(BODY_OWNER, designation)
    }

    val PROPERTY: StateKeeper<FirProperty, FirDesignation> = stateKeeper { builder, property, designation ->
        builder.entity(property.getter, PROPERTY_ACCESSOR, designation)
        builder.entity(property.setter, PROPERTY_ACCESSOR, designation)
    }
}