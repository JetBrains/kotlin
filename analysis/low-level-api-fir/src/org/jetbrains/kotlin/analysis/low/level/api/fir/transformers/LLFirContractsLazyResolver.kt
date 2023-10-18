/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.blockGuard
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkContractDescriptionIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isCallableWithSpecialBody
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.util.PrivateForInline

internal object LLFirContractsLazyResolver : LLFirLazyResolver(FirResolvePhase.CONTRACTS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirContractsTargetResolver(target, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolveDesignation()
    }

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        if (target !is FirContractDescriptionOwner) return
        checkContractDescriptionIsResolved(target)
    }
}

private class LLFirContractsTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    firResolveContextCollector: FirResolveContextCollector?,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.CONTRACTS,
) {
    override val transformer = FirContractResolveTransformer(
        session,
        scopeSession,
        firResolveContextCollector = firResolveContextCollector,
    )

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        collectTowerDataContext(target)

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


    private inline fun actionWithContextCollector(
        noinline action: () -> Unit,
        crossinline collect: (FirResolveContextCollector, BodyResolveContext) -> Unit,
    ): () -> Unit {
        val collector = transformer.firResolveContextCollector ?: return action
        return {
            collect(collector, transformer.context)
            action()
        }
    }

    override fun withScript(firScript: FirScript, action: () -> Unit) {
        val actionWithCollector = actionWithContextCollector(action) { collector, context ->
            collector.addDeclarationContext(firScript, context)
        }

        super.withScript(firScript, actionWithCollector)
    }

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        val actionWithCollector = actionWithContextCollector(action) { collector, context ->
            collector.addFileContext(firFile, context.towerDataContext)
        }

        super.withFile(firFile, actionWithCollector)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        val actionWithCollector = actionWithContextCollector(action) { collector, context ->
            collector.addDeclarationContext(firClass, context)
        }

        @Suppress("DEPRECATION_ERROR")
        super.withRegularClassImpl(firClass, actionWithCollector)
    }

    private fun collectTowerDataContext(target: FirElementWithResolveState) {
        val contextCollector = transformer.firResolveContextCollector
        if (contextCollector == null || target !is FirDeclaration) return

        val bodyResolveContext = transformer.context
        withTypeParametersIfMemberDeclaration(bodyResolveContext, target) {
            when (target) {
                is FirRegularClass -> {
                    contextCollector.addClassHeaderContext(target, bodyResolveContext.towerDataContext)
                }

                is FirFunction -> bodyResolveContext.forFunctionBody(target, transformer.components) {
                    contextCollector.addDeclarationContext(target, bodyResolveContext)
                    for (valueParameter in target.valueParameters) {
                        bodyResolveContext.withValueParameter(valueParameter, transformer.session) {
                            contextCollector.addDeclarationContext(valueParameter, bodyResolveContext)
                        }
                    }
                }

                is FirScript -> {}

                else -> contextCollector.addDeclarationContext(target, bodyResolveContext)
            }
        }

        /**
         * [withRegularClass] and [withScript] already have [FirResolveContextCollector.addDeclarationContext] call,
         * so we shouldn't do anything inside
         */
        when (target) {
            is FirRegularClass -> withRegularClass(target) { }
            is FirScript -> withScript(target) { }
            else -> {}
        }
    }

    private inline fun withTypeParametersIfMemberDeclaration(
        context: BodyResolveContext,
        target: FirElementWithResolveState,
        action: () -> Unit,
    ) {
        if (target is FirMemberDeclaration) {
            @OptIn(PrivateForInline::class)
            context.withTypeParametersOf(target, action)
        } else {
            action()
        }
    }
}

private object ContractStateKeepers {
    private val CONTRACT_DESCRIPTION_OWNER: StateKeeper<FirContractDescriptionOwner, FirDesignationWithFile> = stateKeeper { _, _ ->
        add(FirContractDescriptionOwner::contractDescription, FirContractDescriptionOwner::replaceContractDescription)
    }

    private val BODY_OWNER: StateKeeper<FirFunction, FirDesignationWithFile> = stateKeeper { declaration, _ ->
        if (declaration is FirContractDescriptionOwner && declaration.contractDescription is FirRawContractDescription) {
            // No need to change the body, contract is declared separately
            return@stateKeeper
        }

        if (!isCallableWithSpecialBody(declaration)) {
            add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
        }
    }

    val SIMPLE_FUNCTION: StateKeeper<FirSimpleFunction, FirDesignationWithFile> = stateKeeper { _, designation ->
        add(CONTRACT_DESCRIPTION_OWNER, designation)
        add(BODY_OWNER, designation)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor, FirDesignationWithFile> = stateKeeper { _, designation ->
        add(CONTRACT_DESCRIPTION_OWNER, designation)
        add(BODY_OWNER, designation)
    }

    private val PROPERTY_ACCESSOR: StateKeeper<FirPropertyAccessor, FirDesignationWithFile> = stateKeeper { _, designation ->
        add(CONTRACT_DESCRIPTION_OWNER, designation)
        add(BODY_OWNER, designation)
    }

    val PROPERTY: StateKeeper<FirProperty, FirDesignationWithFile> = stateKeeper { property, designation ->
        entity(property.getter, PROPERTY_ACCESSOR, designation)
        entity(property.setter, PROPERTY_ACCESSOR, designation)
    }
}