/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.codeFragmentScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDelegatedConstructorIsResolved
import org.jetbrains.kotlin.fir.*
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
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForClass
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForFile
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForScript
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractsDslNames
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal object LLFirBodyLazyResolver : LLFirLazyResolver(FirResolvePhase.BODY_RESOLVE) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirBodyTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirValueParameter -> checkDefaultValueIsResolved(target)
            is FirVariable -> checkInitializerIsResolved(target)
            is FirConstructor -> {
                checkDelegatedConstructorIsResolved(target)
                checkBodyIsResolved(target)
            }
            is FirFunction -> checkBodyIsResolved(target)
        }
    }
}

private class LLFirBodyTargetResolver(target: LLFirResolveTarget) : LLFirAbstractBodyTargetResolver(
    target,
    FirResolvePhase.BODY_RESOLVE,
) {
    override val transformer = object : FirBodyResolveTransformer(
        resolveTargetSession,
        phase = resolverPhase,
        implicitTypeOnly = false,
        scopeSession = resolveTargetScopeSession,
        returnTypeCalculator = createReturnTypeCalculator(),
    ) {
        override val preserveCFGForClasses: Boolean get() = false
        override val buildCfgForScripts: Boolean get() = false
        override val buildCfgForFiles: Boolean get() = false

        /**
         * It is safe to resolve foreign annotations on demand because the contract allows it
         * ([annotation arguments][FirResolvePhase.ANNOTATION_ARGUMENTS] phase is less than [body][FirResolvePhase.BODY_RESOLVE] phase).
         */
        override fun transformForeignAnnotationCall(symbol: FirBasedSymbol<*>, annotationCall: FirAnnotationCall): FirAnnotationCall {
            // It is possible that some members of local classes will propagate annotations between each other,
            // so we should just skip them, as they will be resolved anyway
            if (symbol.cannotResolveAnnotationsOnDemand()) return annotationCall

            symbol.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
            checkAnnotationCallIsResolved(symbol, annotationCall)
            return annotationCall
        }
    }

    /**
     * No one should depend on body resolution of another declaration
     */
    override val skipDependencyTargetResolutionStep: Boolean get() = true

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

            is FirFile -> {
                if (target.resolvePhase >= resolverPhase) return true

                target.annotationsContainer?.lazyResolveToPhase(resolverPhase)

                // resolve file CFG graph here, to do this we need to have property blocks resoled
                resolveMembersForControlFlowGraph(target)
                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is FirScript -> {
                if (target.resolvePhase >= resolverPhase) return true

                // resolve properties so they are available for CFG building in resolveScript
                resolveMembersForControlFlowGraph(target)
                performCustomResolveUnderLock(target) {
                    resolve(target, BodyStateKeepers.SCRIPT)
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

        return false
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

    private fun calculateControlFlowGraph(target: FirFile) {
        checkWithAttachment(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the file phase < $resolverPhase)" },
        ) {
            withFirEntry("firFile", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterFile(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitFile()
            ?: errorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
                withFirEntry("firFile", target)
            }

        target.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    private fun resolveMembersForControlFlowGraph(target: FirFile) {
        withFile(target) {
            for (member in target.declarations) {
                if (member is FirControlFlowGraphOwner && member.isUsedInControlFlowGraphBuilderForFile) {
                    member.lazyResolveToPhase(resolverPhase.previous)
                    performResolve(member)
                }
            }
        }
    }

    private fun calculateControlFlowGraph(target: FirScript) {
        checkWithAttachment(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the script phase < $resolverPhase)" },
        ) {
            withFirEntry("firScript", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterScript(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitScript()
            ?: errorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
                withFirEntry("firScript", target)
            }

        target.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    private fun resolveMembersForControlFlowGraph(target: FirScript) {
        withScript(target) {
            for (member in target.declarations) {
                if (member is FirControlFlowGraphOwner && member.isUsedInControlFlowGraphBuilderForScript) {
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
            val contextModule = resolveSession.getModule(contextKtFile)
            val contextSession = resolveSession.sessionProvider.getResolvableSession(contextModule)
            val contextFirFile = resolveSession.getOrBuildFirFile(contextKtFile)

            val sessionHolder = SessionHolderImpl(contextSession, contextSession.getScopeSession())
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
        // There is no sense to resolve such declarations as they do not have bodies
        // Also, they have STUB expression instead of default values, so we shouldn't change them
        if (target is FirCallableDeclaration && target.isCopyCreatedInScope) return

        when (target) {
            is FirFile, is FirScript, is FirRegularClass, is FirCodeFragment -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is FirConstructor -> resolve(target, BodyStateKeepers.CONSTRUCTOR)
            is FirFunction -> resolve(target, BodyStateKeepers.FUNCTION)
            is FirProperty -> resolve(target, BodyStateKeepers.PROPERTY)
            is FirField -> resolve(target, BodyStateKeepers.FIELD)
            is FirVariable -> resolve(target, BodyStateKeepers.VARIABLE)
            is FirAnonymousInitializer -> resolve(target, BodyStateKeepers.ANONYMOUS_INITIALIZER)
            is FirDanglingModifierList,
            is FirFileAnnotationsContainer,
            is FirTypeAlias,
            -> {
                // No bodies here
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }

    override fun rawResolve(target: FirElementWithResolveState) {
        when (target) {
            is FirScript -> {
                resolveScript(target)
                calculateControlFlowGraph(target)
            }

            else -> super.rawResolve(target)
        }

        LLFirDeclarationModificationService.bodyResolved(target, resolverPhase)
    }

    private fun resolveScript(script: FirScript) {
        transformer.declarationsTransformer.withScript(script) {
            script.parameters.forEach { it.transformSingle(transformer, ResolutionMode.ContextIndependent) }
            script
        }
    }
}

internal object BodyStateKeepers {
    val SCRIPT: StateKeeper<FirScript, FirDesignation> = stateKeeper { _, _ ->
        add(FirScript::controlFlowGraphReference, FirScript::replaceControlFlowGraphReference)
    }

    val CODE_FRAGMENT: StateKeeper<FirCodeFragment, FirDesignation> = stateKeeper { _, _ ->
        add(FirCodeFragment::block, FirCodeFragment::replaceBlock, ::blockGuard)
    }

    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer, FirDesignation> = stateKeeper { _, _ ->
        add(FirAnonymousInitializer::body, FirAnonymousInitializer::replaceBody, ::blockGuard)
        add(FirAnonymousInitializer::controlFlowGraphReference, FirAnonymousInitializer::replaceControlFlowGraphReference)
    }

    val FUNCTION: StateKeeper<FirFunction, FirDesignation> = stateKeeper { function, designation ->
        if (function.isCertainlyResolved) {
            if (!isCallableWithSpecialBody(function)) {
                entityList(function.valueParameters, VALUE_PARAMETER, designation)
            }

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

    val CONSTRUCTOR: StateKeeper<FirConstructor, FirDesignation> = stateKeeper { _, designation ->
        add(FUNCTION, designation)
        add(FirConstructor::delegatedConstructor, FirConstructor::replaceDelegatedConstructor, ::delegatedConstructorCallGuard)
    }

    val VARIABLE: StateKeeper<FirVariable, FirDesignation> = stateKeeper { variable, _ ->
        add(FirVariable::returnTypeRef, FirVariable::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(variable)) {
            add(FirVariable::initializerIfUnresolved, FirVariable::replaceInitializer, ::expressionGuard)
            add(FirVariable::delegateIfUnresolved, FirVariable::replaceDelegate, ::expressionGuard)
        }
    }

    private val VALUE_PARAMETER: StateKeeper<FirValueParameter, FirDesignation> = stateKeeper { valueParameter, _ ->
        if (valueParameter.defaultValue != null) {
            add(FirValueParameter::defaultValue, FirValueParameter::replaceDefaultValue, ::expressionGuard)
        }

        add(FirValueParameter::controlFlowGraphReference, FirValueParameter::replaceControlFlowGraphReference)
    }

    val FIELD: StateKeeper<FirField, FirDesignation> = stateKeeper { _, designation ->
        add(VARIABLE, designation)
        add(FirField::controlFlowGraphReference, FirField::replaceControlFlowGraphReference)
    }

    val PROPERTY: StateKeeper<FirProperty, FirDesignation> = stateKeeper { property, designation ->
        if (property.bodyResolveState >= FirPropertyBodyResolveState.ALL_BODIES_RESOLVED) {
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
private fun StateKeeperScope<FirFunction, FirDesignation>.preserveContractBlock(function: FirFunction) {
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
                isSetter -> FirPropertyBodyResolveState.ALL_BODIES_RESOLVED
                else -> FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
            }

            if (propertySymbol.fir.bodyResolveState >= requiredState) {
                return true
            }
        }

        val body = this.body ?: return false // Not completely sure
        return body !is FirLazyBlock && body.isResolved
    }

private val FirVariable.initializerIfUnresolved: FirExpression?
    get() = when (this) {
        is FirProperty -> if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_RESOLVED) initializer else null
        else -> initializer
    }

private val FirVariable.delegateIfUnresolved: FirExpression?
    get() = when (this) {
        is FirProperty -> if (bodyResolveState < FirPropertyBodyResolveState.ALL_BODIES_RESOLVED) delegate else null
        else -> delegate
    }

private val FirProperty.backingFieldIfUnresolved: FirBackingField?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_RESOLVED) getExplicitBackingField() else null

private val FirProperty.getterIfUnresolved: FirPropertyAccessor?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) getter else null

private val FirProperty.setterIfUnresolved: FirPropertyAccessor?
    get() = if (bodyResolveState < FirPropertyBodyResolveState.ALL_BODIES_RESOLVED) setter else null

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

private class LLFirCodeFragmentContext(
    override val towerDataContext: FirTowerDataContext,
    override val smartCasts: Map<RealVariable, Set<ConeKotlinType>>,
) : FirCodeFragmentContext
