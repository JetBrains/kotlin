/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDelegatedConstructorIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractsDslNames
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForClass

internal object LLFirBodyLazyResolver : LLFirLazyResolver(FirResolvePhase.BODY_RESOLVE) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirBodyTargetResolver(target, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = true)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        when (target) {
            is FirValueParameter -> checkDefaultValueIsResolved(target)
            is FirVariable -> checkInitializerIsResolved(target)
            is FirConstructor -> {
                checkDelegatedConstructorIsResolved(target)
                checkBodyIsResolved(target)
            }
            is FirFunction -> checkBodyIsResolved(target)
        }

        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirBodyTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    firResolveContextCollector: FirResolveContextCollector?,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.BODY_RESOLVE,
) {
    override val transformer = object : FirBodyResolveTransformer(
        session,
        phase = resolverPhase,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
        returnTypeCalculator = createReturnTypeCalculator(firResolveContextCollector = firResolveContextCollector),
        firResolveContextCollector = firResolveContextCollector,
    ) {
        override val preserveCFGForClasses: Boolean get() = false
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        when (target) {
            is FirRegularClass -> {
                if (target.resolvePhase >= resolverPhase) return true

                // resolve class CFG graph here, to do this we need to have property & init blocks resoled
                resolveMembersForControlFlowGraph(target)
                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }
        }

        return false
    }

    private fun calculateControlFlowGraph(target: FirRegularClass) {
        checkWithAttachmentBuilder(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the class phase < $resolverPhase)" },
        ) {
            withFirEntry("firClass", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterClass(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitClass()
            ?: buildErrorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
                withFirEntry("firClass", target)
            }

        target.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    private fun resolveMembersForControlFlowGraph(target: FirRegularClass) {
        withRegularClass(target) {
            for (member in target.declarations) {
                if (member is FirControlFlowGraphOwner && member.isUsedInControlFlowGraphBuilderForClass) {
                    member.lazyResolveToPhase(resolverPhase.previous)
                    performResolve(member)
                }
            }
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is FirConstructor -> resolve(target, BodyStateKeepers.CONSTRUCTOR)
            is FirFunction -> resolve(target, BodyStateKeepers.FUNCTION)
            is FirProperty -> resolve(target, BodyStateKeepers.PROPERTY)
            is FirField -> resolve(target, BodyStateKeepers.FIELD)
            is FirVariable -> resolve(target, BodyStateKeepers.VARIABLE)
            is FirAnonymousInitializer -> resolve(target, BodyStateKeepers.ANONYMOUS_INITIALIZER)
            is FirScript -> resolve(target, BodyStateKeepers.SCRIPT)
            is FirDanglingModifierList,
            is FirFileAnnotationsContainer,
            is FirTypeAlias -> {
                // No bodies here
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}

internal object BodyStateKeepers {
    val SCRIPT: StateKeeper<FirScript> = stateKeeper {
        // TODO Lazy body is not supported for scripts yet
    }

    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer> = stateKeeper {
        add(FirAnonymousInitializer::body, FirAnonymousInitializer::replaceBody, ::blockGuard)
        add(FirAnonymousInitializer::controlFlowGraphReference, FirAnonymousInitializer::replaceControlFlowGraphReference)
    }

    val FUNCTION: StateKeeper<FirFunction> = stateKeeper { function ->
        if (function.isCertainlyResolved) {
            return@stateKeeper
        }

        add(FirFunction::returnTypeRef, FirFunction::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(function)) {
            preserveContractBlock(function)

            add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
            entityList(function.valueParameters, VALUE_PARAMETER)
        }

        add(FirFunction::controlFlowGraphReference, FirFunction::replaceControlFlowGraphReference)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor> = stateKeeper {
        add(FUNCTION)
        add(FirConstructor::delegatedConstructor, FirConstructor::replaceDelegatedConstructor, ::delegatedConstructorCallGuard)
    }

    val VARIABLE: StateKeeper<FirVariable> = stateKeeper { variable ->
        add(FirVariable::returnTypeRef, FirVariable::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(variable)) {
            add(FirVariable::initializerIfUnresolved, FirVariable::replaceInitializer, ::expressionGuard)
            add(FirVariable::delegateIfUnresolved, FirVariable::replaceDelegate, ::expressionGuard)
        }
    }

    private val VALUE_PARAMETER: StateKeeper<FirValueParameter> = stateKeeper { valueParameter ->
        if (valueParameter.defaultValue != null) {
            add(FirValueParameter::defaultValue, FirValueParameter::replaceDefaultValue, ::expressionGuard)
        }

        add(FirValueParameter::controlFlowGraphReference, FirValueParameter::replaceControlFlowGraphReference)
    }

    val FIELD: StateKeeper<FirField> = stateKeeper {
        add(VARIABLE)
        add(FirField::controlFlowGraphReference, FirField::replaceControlFlowGraphReference)
    }

    val PROPERTY: StateKeeper<FirProperty> = stateKeeper { property ->
        if (property.bodyResolveState >= FirPropertyBodyResolveState.EVERYTHING_RESOLVED) {
            return@stateKeeper
        }

        add(VARIABLE)

        add(FirProperty::bodyResolveState, FirProperty::replaceBodyResolveState)
        add(FirProperty::returnTypeRef, FirProperty::replaceReturnTypeRef)

        entity(property.getterIfUnresolved, FUNCTION)
        entity(property.setterIfUnresolved, FUNCTION)
        entity(property.backingFieldIfUnresolved, VARIABLE)

        add(FirProperty::controlFlowGraphReference, FirProperty::replaceControlFlowGraphReference)
    }
}

context(StateKeeperBuilder)
private fun StateKeeperScope<FirFunction>.preserveContractBlock(function: FirFunction) {
    val oldBody = function.body
    if (oldBody == null || oldBody is FirLazyBlock) {
        return
    }

    val oldFirstStatement = oldBody.statements.firstOrNull() ?: return

    // The old body starts with a contract definition
    if (oldFirstStatement is FirContractCallBlock) {
        if (oldFirstStatement.call.calleeReference is FirResolvedNamedReference) {
            postProcess {
                val newBody = function.body
                if (newBody != null && newBody.statements.isNotEmpty()) {
                    // Replace the newly created (and not yet resolved) contract block with the old, resolved one
                    newBody.replaceFirstStatement<FirContractCallBlock> { oldFirstStatement }
                }
            }
        }

        return
    }

    // The old body starts with a contract-like call (but it's not a proper contract definition)
    if (oldFirstStatement is FirFunctionCall && oldFirstStatement.calleeReference.name == FirContractsDslNames.CONTRACT.callableName) {
        postProcess {
            val newBody = function.body
            if (newBody != null && newBody.statements.isNotEmpty()) {
                val newFirstStatement = newBody.statements.first()
                if (newFirstStatement is FirContractCallBlock) {
                    // We already know that the function doesn't have a contract, so we can safely unwrap the contract block
                    newBody.replaceFirstStatement<FirContractCallBlock> { newFirstStatement.call }
                }
            }
        }
    }
}

private val FirFunction.isCertainlyResolved: Boolean
    get() {
        if (this is FirPropertyAccessor) {
            val requiredState = when {
                isSetter -> FirPropertyBodyResolveState.EVERYTHING_RESOLVED
                else -> FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
            }

            if (propertySymbol.fir.bodyResolveState >= requiredState) {
                return true
            }
        }

        val body = this.body ?: return false // Not completely sure
        return body !is FirLazyBlock && body.typeRef is FirResolvedTypeRef
    }

private val FirVariable.initializerIfUnresolved: FirExpression?
    get() = when (this) {
        is FirProperty -> if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_RESOLVED) initializer else null
        else -> initializer
    }

private val FirVariable.delegateIfUnresolved: FirExpression?
    get() = when (this) {
        is FirProperty -> if (bodyResolveState < FirPropertyBodyResolveState.EVERYTHING_RESOLVED) delegate else null
        else -> delegate
    }

private val FirProperty.backingFieldIfUnresolved: FirBackingField?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_RESOLVED) getExplicitBackingField() else null

private val FirProperty.getterIfUnresolved: FirPropertyAccessor?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) getter else null

private val FirProperty.setterIfUnresolved: FirPropertyAccessor?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.EVERYTHING_RESOLVED) setter else null

private fun delegatedConstructorCallGuard(fir: FirDelegatedConstructorCall): FirDelegatedConstructorCall {
    if (fir is FirLazyDelegatedConstructorCall) {
        return fir
    } else if (fir is FirMultiDelegatedConstructorCall) {
        return buildMultiDelegatedConstructorCall {
            for (delegatedConstructorCall in fir.delegatedConstructorCalls) {
                delegatedConstructorCalls.add(delegatedConstructorCallGuard(delegatedConstructorCall))
            }
        }
    }

    return buildLazyDelegatedConstructorCall {
        constructedTypeRef = fir.constructedTypeRef
        when (val originalCalleeReference = fir.calleeReference) {
            is FirThisReference -> {
                isThis = true
                calleeReference = buildExplicitThisReference {
                    source = null
                }
            }
            is FirSuperReference -> {
                isThis = false
                calleeReference = buildExplicitSuperReference {
                    source = originalCalleeReference.source
                    superTypeRef = originalCalleeReference.superTypeRef
                }
            }
        }
    }
}