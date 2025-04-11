/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.codeFragmentScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.canHaveDeferredReturnTypeCalculation
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
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.codeFragmentContext
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForClass
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForFile
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForScript
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
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

/**
 * This resolver is responsible for [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE] phase.
 *
 * This resolver:
 * - Transforms bodies of declarations.
 * - Builds [control flow graph][org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph].
 *
 * Before the transformation, the resolver [recreates][BodyStateKeepers] all bodies
 * to prevent corrupted states due to [PCE][com.intellij.openapi.progress.ProcessCanceledException].
 *
 * Special rules:
 * - [FirFile] – All members which [isUsedInControlFlowGraphBuilderForFile] have
 *   to be resolved before the file to build correct [CFG][org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph].
 * - [FirScript] – All members which [isUsedInControlFlowGraphBuilderForScript] have
 *   to be resolved before the script to build correct [CFG][org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph].
 * - [FirRegularClass] – All members which [isUsedInControlFlowGraphBuilderForClass] have
 *   to be resolved before the class to build correct [CFG][org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph].
 *
 * @see BodyStateKeepers
 * @see FirBodyResolveTransformer
 * @see FirResolvePhase.BODY_RESOLVE
 */
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
                resolveMembersForControlFlowGraph(
                    declarationWithMembers = target,
                    withDeclaration = this::withRegularClass,
                    declarationsProvider = FirRegularClass::declarations,
                    isUsedInControlFlowBuilder = FirDeclaration::isUsedInClassControlFlowGraphBuilder,
                )

                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is FirFile -> {
                if (target.resolvePhase >= resolverPhase) return true

                // resolve file CFG graph here, to do this we need to have property blocks resoled
                resolveMembersForControlFlowGraph(
                    declarationWithMembers = target,
                    withDeclaration = this::withFile,
                    declarationsProvider = FirFile::declarations,
                    isUsedInControlFlowBuilder = FirDeclaration::isUsedInFileControlFlowGraphBuilder,
                )

                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is FirScript -> {
                if (target.resolvePhase >= resolverPhase) return true

                // resolve properties so they are available for CFG building
                resolveMembersForControlFlowGraph(
                    declarationWithMembers = target,
                    withDeclaration = this::withScript,
                    declarationsProvider = FirScript::declarations,
                    isUsedInControlFlowBuilder = FirDeclaration::isUsedInScriptControlFlowGraphBuilder,
                )

                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is FirCodeFragment -> {
                val context = resolveCodeFragmentContext(target)
                performCustomResolveUnderLock(target) {
                    target.codeFragmentContext = context
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

    private inline fun <T : FirElementWithResolveState> resolveMembersForControlFlowGraph(
        declarationWithMembers: T,
        withDeclaration: (T, () -> Unit) -> Unit,
        declarationsProvider: (T) -> List<FirDeclaration>,
        crossinline isUsedInControlFlowBuilder: (FirDeclaration) -> Boolean,
    ) {
        val declarations = declarationsProvider(declarationWithMembers)
        if (declarations.none(isUsedInControlFlowBuilder)) return

        withDeclaration(declarationWithMembers) {
            for (declaration in declarations) {
                if (isUsedInControlFlowBuilder(declaration)) {
                    declaration.lazyResolveToPhase(resolverPhase.previous)
                    performResolve(declaration)
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

    @OptIn(DelicateScopeAPI::class)
    private fun resolveCodeFragmentContext(firCodeFragment: FirCodeFragment): LLFirCodeFragmentContext {
        val ktCodeFragment = firCodeFragment.psi as? KtCodeFragment
            ?: errorWithAttachment("Code fragment source not found") {
                withFirEntry("firCodeFragment", firCodeFragment)
            }

        val module = firCodeFragment.llFirModuleData.ktModule
        val resolutionFacade = module.getResolutionFacade(ktCodeFragment.project)

        fun FirTowerDataContext.withExtraScopes(): FirTowerDataContext {
            return resolutionFacade.useSiteFirSession.codeFragmentScopeProvider.getExtraScopes(ktCodeFragment)
                .fold(this) { context, scope ->
                    val scopeWithProperSession = scope.withReplacedSessionOrNull(resolveTargetSession, resolveTargetScopeSession) ?: scope
                    context.addLocalScope(scopeWithProperSession)
                }
        }

        val contextPsiElement = ktCodeFragment.context
        val contextKtFile = contextPsiElement?.containingFile as? KtFile

        return if (contextKtFile != null) {
            val contextModule = resolutionFacade.getModule(contextKtFile)
            val contextSession = resolutionFacade.sessionProvider.getResolvableSession(contextModule)
            val contextFirFile = resolutionFacade.getOrBuildFirFile(contextKtFile)

            val sessionHolder = SessionHolderImpl(contextSession, contextSession.getScopeSession())
            val elementContext = ContextCollector.process(contextFirFile, sessionHolder, contextPsiElement)
                ?: errorWithAttachment("Cannot find enclosing context for ${contextPsiElement::class}") {
                    withPsiEntry("contextPsiElement", contextPsiElement)
                }

            LLFirCodeFragmentContext(
                elementContext.towerDataContext.withProperSession(resolveTargetSession, resolveTargetScopeSession)
                    .withExtraScopes(),
                elementContext.smartCasts
            )
        } else {
            val towerDataContext = FirTowerDataContext().withExtraScopes()
            LLFirCodeFragmentContext(towerDataContext, emptyMap())
        }
    }

    @DelicateScopeAPI
    private fun FirTowerDataContext.withProperSession(session: FirSession, scopeSession: ScopeSession): FirTowerDataContext {
        return replaceTowerDataElements(
            towerDataElements.map { it.withProperSession(session, scopeSession) }.toPersistentList(),
            nonLocalTowerDataElements.map { it.withProperSession(session, scopeSession) }.toPersistentList(),
        )
    }

    @DelicateScopeAPI
    private fun FirTowerDataElement.withProperSession(
        session: FirSession,
        scopeSession: ScopeSession,
    ): FirTowerDataElement = FirTowerDataElement(
        scope?.withReplacedSessionOrNull(session, scopeSession) ?: scope,
        implicitReceiver?.withReplacedSessionOrNull(session, scopeSession),
        contextReceiverGroup?.map { it.withReplacedSessionOrNull(session, scopeSession) },
        contextParameterGroup,
        isLocal,
        staticScopeOwnerSymbol
    )

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // There is no sense to resolve such declarations as they do not have bodies
        // Also, they have STUB expression instead of default values, so we shouldn't change them
        if (target is FirCallableDeclaration && target.canHaveDeferredReturnTypeCalculation) return

        when (target) {
            is FirFile, is FirScript, is FirRegularClass, is FirCodeFragment -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is FirConstructor -> resolve(target, BodyStateKeepers.CONSTRUCTOR)
            is FirFunction -> resolve(target, BodyStateKeepers.FUNCTION)
            is FirProperty -> resolve(target, BodyStateKeepers.PROPERTY)
            is FirField -> resolve(target, BodyStateKeepers.FIELD)
            is FirVariable -> resolve(target, BodyStateKeepers.VARIABLE)
            is FirAnonymousInitializer -> resolve(target, BodyStateKeepers.ANONYMOUS_INITIALIZER)
            is FirDanglingModifierList,
            is FirTypeAlias,
                -> {
                // No bodies here
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }

    override fun rawResolve(target: FirElementWithResolveState) {
        super.rawResolve(target)

        LLFirDeclarationModificationService.bodyResolved(target, resolverPhase)
    }
}

internal object BodyStateKeepers {
    val CODE_FRAGMENT: StateKeeper<FirCodeFragment, FirDesignation> = stateKeeper { builder, _, _ ->
        builder.add(FirCodeFragment::block, FirCodeFragment::replaceBlock, ::blockGuard)
    }

    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer, FirDesignation> = stateKeeper { builder, _, _ ->
        builder.add(FirAnonymousInitializer::body, FirAnonymousInitializer::replaceBody, ::blockGuard)
        builder.add(FirAnonymousInitializer::controlFlowGraphReference, FirAnonymousInitializer::replaceControlFlowGraphReference)
    }

    val FUNCTION: StateKeeper<FirFunction, FirDesignation> = stateKeeper { builder, function, designation ->
        if (function.isCertainlyResolved) {
            if (!isCallableWithSpecialBody(function)) {
                builder.entityList(function.valueParameters, VALUE_PARAMETER, designation)
            }

            return@stateKeeper
        }

        builder.add(FirFunction::returnTypeRef, FirFunction::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(function)) {
            preserveContractBlock(builder, function)

            builder.add(FirFunction::body, FirFunction::replaceBody, ::blockGuard)
            builder.entityList(function.valueParameters, VALUE_PARAMETER, designation)
        }

        builder.add(FirFunction::controlFlowGraphReference, FirFunction::replaceControlFlowGraphReference)
    }

    val CONSTRUCTOR: StateKeeper<FirConstructor, FirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(FUNCTION, designation)
        builder.add(FirConstructor::delegatedConstructor, FirConstructor::replaceDelegatedConstructor, ::delegatedConstructorCallGuard)
    }

    val VARIABLE: StateKeeper<FirVariable, FirDesignation> = stateKeeper { builder, variable, _ ->
        builder.add(FirVariable::returnTypeRef, FirVariable::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(variable)) {
            builder.add(FirVariable::initializerIfUnresolved, FirVariable::replaceInitializer, ::expressionGuard)
            builder.add(FirVariable::delegateIfUnresolved, FirVariable::replaceDelegate, ::expressionGuard)
        }
    }

    private val VALUE_PARAMETER: StateKeeper<FirValueParameter, FirDesignation> = stateKeeper { builder, valueParameter, _ ->
        if (valueParameter.defaultValue != null) {
            builder.add(FirValueParameter::defaultValue, FirValueParameter::replaceDefaultValue, ::expressionGuard)
        }

        builder.add(FirValueParameter::controlFlowGraphReference, FirValueParameter::replaceControlFlowGraphReference)
    }

    val FIELD: StateKeeper<FirField, FirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(VARIABLE, designation)
        builder.add(FirField::controlFlowGraphReference, FirField::replaceControlFlowGraphReference)
    }

    val PROPERTY: StateKeeper<FirProperty, FirDesignation> = stateKeeper { builder, property, designation ->
        if (property.bodyResolveState >= FirPropertyBodyResolveState.ALL_BODIES_RESOLVED) {
            return@stateKeeper
        }

        builder.add(VARIABLE, designation)

        builder.add(FirProperty::bodyResolveState, FirProperty::replaceBodyResolveState)
        builder.add(FirProperty::returnTypeRef, FirProperty::replaceReturnTypeRef)

        builder.entity(property.getterIfUnresolved, FUNCTION, designation)
        builder.entity(property.setterIfUnresolved, FUNCTION, designation)
        builder.entity(property.backingFieldIfUnresolved, VARIABLE, designation)

        builder.add(FirProperty::controlFlowGraphReference, FirProperty::replaceControlFlowGraphReference)
    }
}

private fun StateKeeperScope<FirFunction, FirDesignation>.preserveContractBlock(builder: StateKeeperBuilder, function: FirFunction) {
    val oldBody = function.body
    if (oldBody == null || oldBody is FirLazyBlock) {
        return
    }

    val oldFirstStatement = oldBody.statements.firstOrNull() ?: return

    // The old body starts with a contract definition
    if (oldFirstStatement is FirContractCallBlock) {
        if (oldFirstStatement.call.calleeReference is FirResolvedNamedReference) {
            builder.postProcess {
                val newBody = function.body
                if (newBody != null && newBody.statements.isNotEmpty()) {
                    // Replace the newly created (and not yet resolved) contract block with the old, resolved one
                    newBody.replaceFirstStatement<FirContractCallBlock> { oldFirstStatement }
                }
            }
        }

        return
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

private val FirDeclaration.isUsedInFileControlFlowGraphBuilder: Boolean
    get() = this is FirControlFlowGraphOwner && isUsedInControlFlowGraphBuilderForFile

private val FirDeclaration.isUsedInScriptControlFlowGraphBuilder: Boolean
    get() = this is FirControlFlowGraphOwner && isUsedInControlFlowGraphBuilderForScript

private val FirDeclaration.isUsedInClassControlFlowGraphBuilder: Boolean
    get() = this is FirControlFlowGraphOwner && isUsedInControlFlowGraphBuilderForClass
