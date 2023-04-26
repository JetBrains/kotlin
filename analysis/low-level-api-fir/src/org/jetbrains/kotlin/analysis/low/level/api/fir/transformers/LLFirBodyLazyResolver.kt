/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkBodyIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDefaultValueIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkInitializerIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

internal object BodyStateKeepers {
    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer> = stateKeeper {
        add(FirAnonymousInitializer::body, FirAnonymousInitializer::replaceBody) { buildLazyBlock() }
    }

    val FUNCTION: StateKeeper<FirFunction> = stateKeeper {
        add(FirFunction::body, FirFunction::replaceBody) { buildLazyBlock() }
        add(FirFunction::returnTypeRef, FirFunction::replaceReturnTypeRef)
        add(FirFunction::controlFlowGraphReference, FirFunction::replaceControlFlowGraphReference)
        addList(FirFunction::valueParameters, FirValueParameter::defaultValue, FirValueParameter::replaceDefaultValue) { fir ->
            buildLazyExpression(fir)
        }
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor> = stateKeeper(FUNCTION) {
        add(FirConstructor::delegatedConstructor, FirConstructor::replaceDelegatedConstructor) { makeLazyDelegatedConstructorCall(it) }
    }

    val PROPERTY: StateKeeper<FirProperty> = stateKeeper {
        // Property initializer is supposed to be either lazy (so it's safe to roll back to it) or fully resolved.
        add(FirProperty::initializer, FirProperty::replaceInitializer)
        add(FirProperty::returnTypeRef, FirProperty::replaceReturnTypeRef)
        addNested(FirProperty::getter, FirPropertyAccessor::body, FirPropertyAccessor::replaceBody) { buildLazyBlock() }
        addNested(FirProperty::setter, FirPropertyAccessor::body, FirPropertyAccessor::replaceBody) { buildLazyBlock() }
        addNested(FirProperty::backingField, FirBackingField::initializer, FirBackingField::replaceInitializer) {
            buildLazyExpression(it)
        }
        addNested(
            { it.delegate as? FirWrappedDelegateExpression },
            FirWrappedDelegateExpression::delegateProvider,
            FirWrappedDelegateExpression::replaceDelegateProvider
        )
        addNested(
            { it.delegate as? FirWrappedDelegateExpression },
            FirWrappedDelegateExpression::expression,
            FirWrappedDelegateExpression::replaceExpression
        )
    }

    val ENUM_ENTRY: StateKeeper<FirEnumEntry> = stateKeeper {
        add(FirEnumEntry::initializer, FirEnumEntry::replaceInitializer) { buildLazyExpression(it) }
    }
}

internal object LLFirBodyLazyResolver : LLFirLazyResolver(FirResolvePhase.BODY_RESOLVE) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
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
    towerDataContextCollector: FirTowerDataContextCollector?,
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
        returnTypeCalculator = createReturnTypeCalculator(),
        firTowerDataContextCollector = towerDataContextCollector,
    ) {
        override val preserveCFGForClasses: Boolean get() = false
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        when (target) {
            is FirRegularClass -> {
                if (target.resolvePhase >= resolverPhase) return true

                withRegularClass(target) {
                    transformer.firTowerDataContextCollector?.addDeclarationContext(target, transformer.context.towerDataContext)
                }

                // resolve class CFG graph here, to do this we need to have property & init blocks resoled
                resolveMemberPropertiesForControlFlowGraph(target)
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

    private fun resolveMemberPropertiesForControlFlowGraph(target: FirRegularClass) {
        withRegularClass(target) {
            for (member in target.declarations) {
                if (member is FirCallableDeclaration || member is FirAnonymousInitializer) {
                    // TODO: Ideally, only properties and init blocks should be resolved here.
                    // However, dues to changes in the compiler resolution, we temporarily have to resolve all callable members.
                    // Such additional work might affect incremental analysis performance.
                    member.lazyResolveToPhase(resolverPhase.previous)

                    performCustomResolveUnderLock(member) {
                        performResolve(member)
                    }
                }
            }
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        val contextCollector = transformer.firTowerDataContextCollector
        if (contextCollector != null && target is FirDeclaration) {
            val bodyResolveContext = transformer.context
            if (target is FirFunction) {
                bodyResolveContext.forFunctionBody(target, transformer.components) {
                    contextCollector.addDeclarationContext(target, bodyResolveContext.towerDataContext)
                }
            } else {
                contextCollector.addDeclarationContext(target, bodyResolveContext.towerDataContext)
            }
        }

        when (target) {
            is FirRegularClass -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is FirDanglingModifierList,
            is FirFileAnnotationsContainer,
            is FirTypeAlias -> {
                // No bodies here
            }
            is FirCallableDeclaration -> resolveBody(target)
            is FirAnonymousInitializer -> resolveBody(target)
            else -> throwUnexpectedFirElementError(target)
        }
    }
}

private fun buildLazyExpression(fir: FirElement): FirLazyExpression {
    return buildLazyExpression {
        source = fir.source
    }
}

private fun makeLazyDelegatedConstructorCall(fir: FirConstructor): FirDelegatedConstructorCall {
    val originalConstructor = fir.delegatedConstructor ?: error("Delegated constructor is missing")
    return buildLazyDelegatedConstructorCall {
        constructedTypeRef = originalConstructor.constructedTypeRef
        when (val originalCalleeReference = originalConstructor.calleeReference) {
            is FirThisReference -> {
                isThis = true
                calleeReference = buildExplicitThisReference {
                    source = null
                }
            }
            is FirSuperReference -> {
                isThis = false
                calleeReference = buildExplicitSuperReference {
                    source = null
                    superTypeRef = originalCalleeReference.superTypeRef
                }
            }
        }
    }
}

