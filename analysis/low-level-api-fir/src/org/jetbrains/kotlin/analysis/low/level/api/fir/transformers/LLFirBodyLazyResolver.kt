/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import com.intellij.psi.util.descendantsOfType
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirPartialBodyResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveRequest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveSnapshot
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.partialBodyResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.codeFragmentScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.isCopyCreatedInScope
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.FirCodeFragmentContext
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.codeFragmentContext
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForClass
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForFile
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isUsedInControlFlowGraphBuilderForScript
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.writeResultType
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.concurrent.CancellationException

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

internal class PartialBodyAnalysisSuspendedException : CancellationException("Partial body analysis was suspended")

private class FirPartialBodyExpressionResolveTransformer(
    transformer: FirAbstractBodyResolveTransformerDispatcher,
    private val target: LLFirResolveTarget
) : FirExpressionsResolveTransformer(transformer) {
    private companion object {
        // After a certain number of partial analyses,
        // trigger the full analysis so we don't return to the same declaration over and over again.
        // Note that the first analysis can also perform only default parameter value analysis and exit just after it.
        private const val MAX_ANALYSES_COUNT = 3
    }

    private var shouldStop = false

    override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): E {
        if (shouldStop) {
            return element
        }

        return super.transformElement(element, data)
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): FirStatement {
        val declaration = context.containerIfAny

        val isApplicable = declaration is FirDeclaration
                && !declaration.isLocalMember
                && declaration.isPartialBodyResolvable
                && declaration.body == block
                && block.statements.size > 1

        if (!isApplicable) {
            return super.transformBlock(block, data)
        }

        require(data is ResolutionMode.ContextIndependent)

        val state = declaration.partialBodyResolveState

        if (target is LLFirPartialBodyResolveTarget && (state == null || state.performedAnalysesCount < MAX_ANALYSES_COUNT)) {
            transformPartially(target.request, block, data, state)
        } else {
            transformFully(declaration, block, data, state)
        }

        return block
    }

    private fun transformPartially(
        request: LLPartialBodyResolveRequest,
        block: FirBlock,
        data: ResolutionMode,
        state: LLPartialBodyResolveState?
    ): FirStatement {
        val declaration = target.target as FirDeclaration

        if (state == null) {
            if (request.stopElement == null) {
                // Without a 'stopElement', we resolve "the rest of the body".
                // We didn't analyze the body yet, though.
                // So here we just delegate to the non-partial implementation.
                require(request.targetPsiStatementCount == request.totalPsiStatementCount)
                return super.transformBlock(block, data)
            }

            context.forBlock(session) {
                dataFlowAnalyzer.enterBlock(block)
                if (transformStatementsPartially(request, block, data, startIndex = 0, performedAnalysesCount = 0)) {
                    dataFlowAnalyzer.exitBlock(block)
                }
            }

            return block
        }

        if (state.analyzedPsiStatementCount >= request.targetPsiStatementCount) {
            // Nothing to analyze
            return block
        }

        val resolveSnapshot = state.analysisStateSnapshot
        checkWithAttachment(resolveSnapshot != null, { "Snapshot should be available for a partially analyzed declaration" }) {
            withFirEntry("target", declaration)
            withEntry("state", state) { it.toString() }
        }

        // Run analysis with the tower data context restored from the snapshot
        context.withTowerDataContext(resolveSnapshot.towerDataContext) {
            // Also, restore the previous state of the data flow graph from the snapshot
            context.dataFlowAnalyzerContext.resetFrom(resolveSnapshot.dataFlowAnalyzerContext)

            val isAnalyzedEntirely = transformStatementsPartially(
                request, block, data,
                startIndex = state.analyzedFirStatementCount,
                performedAnalysesCount = state.performedAnalysesCount
            )

            if (isAnalyzedEntirely) {
                dataFlowAnalyzer.exitBlock(block)
            }
        }

        return block
    }

    private fun transformStatementsPartially(
        request: LLPartialBodyResolveRequest,
        block: FirBlock,
        data: ResolutionMode,
        startIndex: Int,
        performedAnalysesCount: Int,
    ): Boolean {
        val declaration = request.target

        val stopElement = request.stopElement
        val stopElements = stopElement?.descendantsOfType<KtElement>(childrenFirst = false)?.toList().orEmpty()

        var index = 0
        val iterator = (block.statements as MutableList<FirStatement>).listIterator()

        while (iterator.hasNext()) {
            val statement = iterator.next()

            if (index >= startIndex) {
                if (stopElement != null && !shouldTransform(statement, stopElement, stopElements)) {
                    // Here we reached a stop element.
                    // It means all statements up to the target one are now analyzed.
                    // So now we save the current context and suspend further analysis.
                    declaration.partialBodyResolveState = LLPartialBodyResolveState(
                        totalPsiStatementCount = request.totalPsiStatementCount,
                        analyzedPsiStatementCount = request.targetPsiStatementCount,
                        analyzedFirStatementCount = index,
                        performedAnalysesCount = performedAnalysesCount + 1,
                        analysisStateSnapshot = LLPartialBodyResolveSnapshot(
                            towerDataContext = context.towerDataContext.createSnapshot(keepMutable = true),
                            dataFlowAnalyzerContext = context.dataFlowAnalyzerContext
                        )
                    )
                    shouldStop = true
                    throw PartialBodyAnalysisSuspendedException()
                    return false
                }

                val newStatement = statement.transform<FirStatement, ResolutionMode>(transformer, data)
                if (statement !== newStatement) {
                    iterator.set(newStatement)
                }
            }

            index += 1
        }

        // Nothing stopped us from analyzing all statements for some reason.
        // Most likely, we missed the stop element.
        // Let's still wrap things out – the function is now fully analyzed.
        block.transformOtherChildren(transformer, data)

        // This makes the compiler think the declaration is fully resolved (see 'FirExpression.isResolved')
        block.writeResultType(session)

        declaration.partialBodyResolveState = LLPartialBodyResolveState(
            totalPsiStatementCount = request.totalPsiStatementCount,
            analyzedPsiStatementCount = request.totalPsiStatementCount,
            analyzedFirStatementCount = index,
            performedAnalysesCount = 1,
            analysisStateSnapshot = null
        )

        return true
    }

    private fun transformFully(
        declaration: FirDeclaration,
        block: FirBlock,
        data: ResolutionMode,
        currentState: LLPartialBodyResolveState?
    ): FirStatement {
        if (currentState == null) {
            // The declaration body is not resolved at all, and a full resolution is requested.
            // So, here we delegate straight to the non-partial implementation.
            return super.transformBlock(block, data)
        }

        val request = LLPartialBodyResolveRequest(
            target = declaration,
            totalPsiStatementCount = currentState.totalPsiStatementCount,
            targetPsiStatementCount = currentState.totalPsiStatementCount,
            stopElement = null
        )

        // Otherwise, use the partial resolve to finish the ongoing resolution
        return transformPartially(request, block, data, currentState)
    }

    private fun shouldTransform(element: FirElement, stopElement: KtElement, stopElements: List<KtElement>): Boolean {
        val source = element.source
        if (source is KtPsiSourceElement) {
            if (source.psi == stopElement) {
                return false
            } else if (source.psi in stopElements) {
                println("found in children!")
                return false
            }
        }

        return true
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
private class LLFirBodyTargetResolver(private val target: LLFirResolveTarget) : LLFirAbstractBodyTargetResolver(
    target,
    FirResolvePhase.BODY_RESOLVE,
) {
    override val transformer = BodyTransformerDispatcher()

    inner class BodyTransformerDispatcher : FirAbstractBodyResolveTransformerDispatcher(
        resolveTargetSession,
        phase = resolverPhase,
        implicitTypeOnly = false,
        scopeSession = resolveTargetSession.getScopeSession(),
        returnTypeCalculator = createReturnTypeCalculator(),
        expandTypeAliases = true
    ) {
        override val expressionsTransformer: FirExpressionsResolveTransformer = FirPartialBodyExpressionResolveTransformer(this, target)
        override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsResolveTransformer(this)

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
        val resolveSession = module.getFirResolveSession(ktCodeFragment.project) as LLFirResolvableResolveSession

        fun FirTowerDataContext.withExtraScopes(): FirTowerDataContext {
            return resolveSession.useSiteFirSession.codeFragmentScopeProvider.getExtraScopes(ktCodeFragment)
                .fold(this) { context, scope ->
                    val scopeWithProperSession = scope.withReplacedSessionOrNull(resolveTargetSession, resolveTargetScopeSession) ?: scope
                    context.addLocalScope(scopeWithProperSession)
                }
        }

        val contextPsiElement = ktCodeFragment.context
        val contextKtFile = contextPsiElement?.containingFile as? KtFile

        return if (contextKtFile != null) {
            val contextModule = resolveSession.getModule(contextKtFile)
            val contextSession = resolveSession.sessionProvider.getResolvableSession(contextModule)
            val contextFirFile = resolveSession.getOrBuildFirFile(contextKtFile)

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
            is FirTypeAlias,
                -> {
                // No bodies here
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }

    override fun rawResolve(target: FirElementWithResolveState) {
        try {
            super.rawResolve(target)
        } catch (e: Throwable) {
            if (e is PartialBodyAnalysisSuspendedException) {
                // We successfully analyzed some part of the body so need to keep track of it
                LLFirDeclarationModificationService.bodyResolved(target, resolverPhase)
            }
            throw e
        }

        LLFirDeclarationModificationService.bodyResolved(target, resolverPhase)
    }
}

internal object BodyStateKeepers {
    val CODE_FRAGMENT: StateKeeper<FirCodeFragment, FirDesignation> = stateKeeper { builder, _, _ ->
        builder.add(FirCodeFragment::block, FirCodeFragment::replaceBlock, ::blockGuard)
    }

    val PARTIAL_BODY_RESOLVABLE: StateKeeper<FirDeclaration, FirDesignation> = stateKeeper { builder, declaration, context ->
        builder.add(
            provider = FirDeclaration::partialBodyResolveState,
            mutator = { _, _ ->
                // On exceptions, remove the partial body resolve state so the function will be analyzed from the beginning
                null
            }
        )
    }

    val ANONYMOUS_INITIALIZER: StateKeeper<FirAnonymousInitializer, FirDesignation> = stateKeeper { builder, initializer, designation ->
        builder.add(PARTIAL_BODY_RESOLVABLE, designation)
        preserveResolvedStatements(builder, initializer)

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
            builder.add(PARTIAL_BODY_RESOLVABLE, designation)
            preserveResolvedStatements(builder, function)

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

@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun StateKeeperScope<FirAnonymousInitializer, FirDesignation>.preserveResolvedStatements(
    builder: StateKeeperBuilder,
    initializer: FirAnonymousInitializer
) {
    preservePartialBodyResolveResult(builder, initializer, FirAnonymousInitializer::body)
}

@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun StateKeeperScope<FirFunction, FirDesignation>.preserveResolvedStatements(builder: StateKeeperBuilder, function: FirFunction) {
    if (preservePartialBodyResolveResult(builder, function, FirFunction::body)) {
        return
    }

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

@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun <T : FirDeclaration> StateKeeperScope<T, FirDesignation>.preservePartialBodyResolveResult(
    builder: StateKeeperBuilder,
    declaration: T,
    bodySupplier: (T) -> FirBlock?,
): Boolean {
    val oldBody = bodySupplier(declaration)
    if (oldBody == null || oldBody is FirLazyBlock) {
        return false
    }

    val state = declaration.partialBodyResolveState
    if (state == null) {
        return false
    }

    builder.postProcess {
        val newBody = bodySupplier(declaration)
        if (newBody != null && newBody.statements.isNotEmpty()) {
            require(oldBody.statements.size == newBody.statements.size) { "Bodies do not match" }

            val newBodyStatements = newBody.statements as MutableList<FirStatement>
            for (index in 0..<state.analyzedFirStatementCount) {
                newBodyStatements[index] = oldBody.statements[index]
            }
        }
    }

    return true
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
    val originalCalleeReference = fir.calleeReference

    if (originalCalleeReference is FirResolvedNamedReference || originalCalleeReference.isError()) {
        // The reference is already resolved – no need in resolving it once more
        return fir
    }

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

        when (originalCalleeReference) {
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
