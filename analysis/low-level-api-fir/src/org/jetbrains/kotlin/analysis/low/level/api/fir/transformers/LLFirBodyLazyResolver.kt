/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.codeFragmentScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDelegatedConstructorIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
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
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.resolve.FirCodeFragmentContext
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.codeFragmentContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractsDslNames
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForClass
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import org.jetbrains.kotlin.utils.findIsInstanceAnd

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

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirValueParameter -> checkDefaultValueIsResolved(target)
            is FirVariable -> checkInitializerIsResolved(target)
            is FirConstructor -> {
                checkDelegatedConstructorIsResolved(target)
                checkBodyIsResolved(target)
            }
            is FirFunction -> checkBodyIsResolved(target)
            is FirScript -> checkStatementsAreResolved(target)
        }
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
            is FirCodeFragment -> {
                resolveCodeFragmentContext(target)
                performCustomResolveUnderLock(target) {
                    resolve(target, BodyStateKeepers.CODE_FRAGMENT)
                }

                return true
            }
        }

        return super.doResolveWithoutLock(target)
    }

    private fun calculateControlFlowGraph(target: FirRegularClass) {
        checkWithAttachment(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the class phase < $resolverPhase)" },
        ) {
            withFirEntry("firClass", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterClass(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitClass()
            ?: errorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
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

    private fun resolveCodeFragmentContext(firCodeFragment: FirCodeFragment) {
        val ktCodeFragment = firCodeFragment.psi as? KtCodeFragment
            ?: errorWithAttachment("Code fragment source not found") {
                withFirEntry("firCodeFragment", firCodeFragment)
            }

        val module = firCodeFragment.llFirModuleData.ktModule
        val resolveSession = module.getFirResolveSession(ktCodeFragment.project) as LLFirResolvableResolveSession

        fun FirTowerDataContext.withExtraScopes(): FirTowerDataContext {
            return resolveSession.useSiteFirSession.codeFragmentScopeProvider.getExtraScopes(ktCodeFragment)
                .fold(this) { context, scope -> context.addLocalScope(scope) }
        }

        val contextPsiElement = ktCodeFragment.context
        val contextKtFile = contextPsiElement?.containingFile as? KtFile

        firCodeFragment.codeFragmentContext = if (contextKtFile != null) {
            val contextFirFile = resolveSession.getOrBuildFirFile(contextKtFile)
            val sessionHolder = transformer.components

            val elementContext = ContextCollector.process(contextFirFile, sessionHolder, contextPsiElement)
                ?: errorWithAttachment("Cannot find enclosing context for ${contextPsiElement::class}") {
                    withPsiEntry("contextPsiElement", contextPsiElement)
                }

            LLFirCodeFragmentContext(elementContext.towerDataContext.withExtraScopes(), elementContext.smartCasts)
        } else {
            val towerDataContext = FirTowerDataContext().withExtraScopes()
            LLFirCodeFragmentContext(towerDataContext, emptyMap())
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass, is FirCodeFragment -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is FirConstructor -> resolve(target, BodyStateKeepers.CONSTRUCTOR)
            is FirFunction -> resolve(target, BodyStateKeepers.FUNCTION)
            is FirProperty -> resolve(target, BodyStateKeepers.PROPERTY)
            is FirField -> resolve(target, BodyStateKeepers.FIELD)
            is FirVariable -> resolve(target, BodyStateKeepers.VARIABLE)
            is FirAnonymousInitializer -> resolve(target, BodyStateKeepers.ANONYMOUS_INITIALIZER)
            is FirScript -> resolve(target, BodyStateKeepers.SCRIPT)
            is FirDanglingModifierList,
            is FirFileAnnotationsContainer,
            is FirTypeAlias,
            is FirFile,
            -> {
                // No bodies here
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }

    override fun rawResolve(target: FirElementWithResolveState) {
        when (target) {
            is FirScript -> target.takeUnless(FirScript::isCertainlyResolved)?.let(::resolveScript)
            else -> super.rawResolve(target)
        }
    }
}

internal object BodyStateKeepers {
    val SCRIPT: StateKeeper<FirScript, FirDesignationWithFile> = stateKeeper { script, designation ->
        val oldStatements = script.statements
        if (oldStatements.none { it.isScriptStatement } || script.isCertainlyResolved) return@stateKeeper

        add(RESULT_PROPERTY, designation)
        add(FirScript::statements, FirScript::replaceStatements) {
            val recreatedStatements = FirLazyBodiesCalculator.createStatementsForScript(script)
            requireSameSize(oldStatements, recreatedStatements)

            ArrayList<FirStatement>(oldStatements.size).apply {
                oldStatements.zip(recreatedStatements).mapTo(this) { (old, new) ->
                    when {
                        !old.isScriptStatement -> old
                        old is FirProperty && old.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> {
                            old.replaceInitializer((new as FirProperty).initializer)
                            old
                        }

                        else -> new
                    }
                }
            }
        }
    }

    private val RESULT_PROPERTY: StateKeeper<FirScript, FirDesignationWithFile> = stateKeeper { script, _ ->
        val resultedProperty = script.findResultProperty() ?: return@stateKeeper
        add(
            provider = { resultedProperty.bodyResolveState },
            mutator = { _, value -> resultedProperty.replaceBodyResolveState(value) },
        )

        add(
            provider = { resultedProperty.returnTypeRef },
            mutator = { _, value -> resultedProperty.replaceReturnTypeRef(value) },
        )

        add(
            provider = { resultedProperty.controlFlowGraphReference },
            mutator = { _, value -> resultedProperty.replaceControlFlowGraphReference(value) },
        )
    }

    val CODE_FRAGMENT: StateKeeper<FirCodeFragment, FirDesignationWithFile> = stateKeeper { _, _ ->
        add(FirCodeFragment::block, FirCodeFragment::replaceBlock, ::blockGuard)
    }

    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer, FirDesignationWithFile> = stateKeeper { _, _ ->
        add(FirAnonymousInitializer::body, FirAnonymousInitializer::replaceBody, ::blockGuard)
        add(FirAnonymousInitializer::controlFlowGraphReference, FirAnonymousInitializer::replaceControlFlowGraphReference)
    }

    val FUNCTION: StateKeeper<FirFunction, FirDesignationWithFile> = stateKeeper { function, designation ->
        if (function.isCertainlyResolved) {
            return@stateKeeper
        }

        add(FirFunction::returnTypeRef, FirFunction::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(function)) {
            preserveContractBlock(function)

            add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
            entityList(function.valueParameters, VALUE_PARAMETER, designation)
        }

        add(FirFunction::controlFlowGraphReference, FirFunction::replaceControlFlowGraphReference)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor, FirDesignationWithFile> = stateKeeper { _, designation ->
        add(FUNCTION, designation)
        add(FirConstructor::delegatedConstructor, FirConstructor::replaceDelegatedConstructor, ::delegatedConstructorCallGuard)
    }

    val VARIABLE: StateKeeper<FirVariable, FirDesignationWithFile> = stateKeeper { variable, _ ->
        add(FirVariable::returnTypeRef, FirVariable::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(variable)) {
            add(FirVariable::initializerIfUnresolved, FirVariable::replaceInitializer, ::expressionGuard)
            add(FirVariable::delegateIfUnresolved, FirVariable::replaceDelegate, ::expressionGuard)
        }
    }

    private val VALUE_PARAMETER: StateKeeper<FirValueParameter, FirDesignationWithFile> = stateKeeper { valueParameter, _ ->
        if (valueParameter.defaultValue != null) {
            add(FirValueParameter::defaultValue, FirValueParameter::replaceDefaultValue, ::expressionGuard)
        }

        add(FirValueParameter::controlFlowGraphReference, FirValueParameter::replaceControlFlowGraphReference)
    }

    val FIELD: StateKeeper<FirField, FirDesignationWithFile> = stateKeeper { _, designation ->
        add(VARIABLE, designation)
        add(FirField::controlFlowGraphReference, FirField::replaceControlFlowGraphReference)
    }

    val PROPERTY: StateKeeper<FirProperty, FirDesignationWithFile> = stateKeeper { property, designation ->
        if (property.bodyResolveState >= FirPropertyBodyResolveState.EVERYTHING_RESOLVED) {
            return@stateKeeper
        }

        add(VARIABLE, designation)

        add(FirProperty::bodyResolveState, FirProperty::replaceBodyResolveState)
        add(FirProperty::returnTypeRef, FirProperty::replaceReturnTypeRef)

        entity(property.getterIfUnresolved, FUNCTION, designation)
        entity(property.setterIfUnresolved, FUNCTION, designation)
        entity(property.backingFieldIfUnresolved, VARIABLE, designation)

        add(FirProperty::controlFlowGraphReference, FirProperty::replaceControlFlowGraphReference)
    }
}

context(StateKeeperBuilder)
private fun StateKeeperScope<FirFunction, FirDesignationWithFile>.preserveContractBlock(function: FirFunction) {
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

private fun FirScript.findResultProperty(): FirProperty? = statements.findIsInstanceAnd<FirProperty> {
    it.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty
}

private val FirScript.isCertainlyResolved: Boolean
    get() {
        val dependentProperty = findResultProperty() ?: return false

        // This meant that we already resolve the entire script on implicit body phase
        return dependentProperty.bodyResolveState == FirPropertyBodyResolveState.EVERYTHING_RESOLVED
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
        return body !is FirLazyBlock && body.coneTypeOrNull != null
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

private fun requireSameSize(old: List<FirStatement>, new: List<FirStatement>) {
    requireWithAttachment(
        condition = old.size == new.size,
        message = { "The number of statements are different" }
    ) {
        withEntryGroup("originalStatements") {
            old.forEachIndexed { index, statement -> withFirEntry("statement$index", statement) }
        }

        withEntryGroup("newStatements") {
            new.forEachIndexed { index, statement -> withFirEntry("statement$index", statement) }
        }
    }
}

private class LLFirCodeFragmentContext(
    override val towerDataContext: FirTowerDataContext,
    override val variables: Map<FirBasedSymbol<*>, Set<ConeKotlinType>>
) : FirCodeFragmentContext
