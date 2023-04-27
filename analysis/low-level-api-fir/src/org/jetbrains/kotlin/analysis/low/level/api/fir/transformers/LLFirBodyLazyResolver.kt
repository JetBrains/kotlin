/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

@Suppress("IncorrectFormatting")
internal object BodyStateKeepers {
    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer> = stateKeeper {
        add(FirAnonymousInitializer::body, FirAnonymousInitializer::replaceBody) { buildLazyBlock() }
    }

    val FUNCTION: StateKeeper<FirFunction> = stateKeeper {
        add(FirFunction::body, FirFunction::replaceBody) { buildLazyBlock() }
        add(FirFunction::controlFlowGraphReference, FirFunction::replaceControlFlowGraphReference)
        addDynamicList(FirFunction::valueParameters, FirValueParameter::defaultValue, FirValueParameter::replaceDefaultValue)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor> = stateKeeper(FUNCTION) {
        add(FirConstructor::delegatedConstructor, FirConstructor::replaceDelegatedConstructor)
    }

    val PROPERTY: StateKeeper<FirProperty> = stateKeeper {
        add(FirProperty::initializeIfUnresolved, FirProperty::replaceInitializer) { buildLazyExpression(it) }
        addDynamic { property ->
            val getter = property.getterIfUnresolved
            if (getter != null) {
                addCustom({ getter }, FirPropertyAccessor::body, FirPropertyAccessor::replaceBody) { buildLazyBlock() }
                addCustom({ getter }, FirPropertyAccessor::returnTypeRef, FirPropertyAccessor::replaceReturnTypeRef)
            }

            val setter = property.setterIfUnresolved
            if (setter != null) {
                addCustom({ setter }, FirPropertyAccessor::body, FirPropertyAccessor::replaceBody) { buildLazyBlock() }
                addCustom({ setter }, FirPropertyAccessor::returnTypeRef, FirPropertyAccessor::replaceReturnTypeRef)
                for (valueParameter in setter.valueParameters) {
                    addCustom({ valueParameter }, FirValueParameter::returnTypeRef, FirValueParameter::replaceReturnTypeRef)
                }
            }

            val backingField = property.backingField
            if (backingField != null) {
                addCustom({ backingField }, FirBackingField::initializer, FirBackingField::replaceInitializer) { buildLazyExpression(it) }
                addCustom({ backingField}, FirBackingField::returnTypeRef, FirBackingField::replaceReturnTypeRef)
            }

            val delegate = property.delegateIfUnresolved
            if (delegate != null) {
                addCustom({ delegate }, FirWrappedDelegateExpression::delegateProvider, FirWrappedDelegateExpression::replaceDelegateProvider)
                addCustom({ delegate }, FirWrappedDelegateExpression::expression, FirWrappedDelegateExpression::replaceExpression)
            }
        }
    }

    val ENUM_ENTRY: StateKeeper<FirEnumEntry> = stateKeeper {
        add(FirEnumEntry::initializer, FirEnumEntry::replaceInitializer) { buildLazyExpression(it) }
    }
}

internal object ImplicitTypeBodyStateKeepers {
    val FUNCTION: StateKeeper<FirFunction> = stateKeeper(BodyStateKeepers.FUNCTION) {
        add(FirFunction::returnTypeRef, FirFunction::replaceReturnTypeRef)
    }

    val PROPERTY: StateKeeper<FirProperty> = stateKeeper(BodyStateKeepers.PROPERTY) {
        add(FirProperty::returnTypeRef, FirProperty::replaceReturnTypeRef)
        addCustom(FirProperty::getter, FirPropertyAccessor::returnTypeRef, FirPropertyAccessor::replaceReturnTypeRef)
        addCustom(FirProperty::setter, FirPropertyAccessor::returnTypeRef, FirPropertyAccessor::replaceReturnTypeRef)
    }
}

private val FirProperty.initializeIfUnresolved: FirExpression?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_RESOLVED) initializer else null

private val FirProperty.getterIfUnresolved: FirPropertyAccessor?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) getter else null

private val FirProperty.setterIfUnresolved: FirPropertyAccessor?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.EVERYTHING_RESOLVED) setter else null

private val FirProperty.delegateIfUnresolved: FirWrappedDelegateExpression?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.EVERYTHING_RESOLVED) delegate as? FirWrappedDelegateExpression else null

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
