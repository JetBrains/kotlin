/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveRequest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.partialBodyResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.buildErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal typealias DeclarationFirElementProvider = (KtElement) -> FirElement?

internal class EagerDeclarationFirElementProvider(firDeclaration: FirDeclaration) : DeclarationFirElementProvider {
    private val mapping = FirElementsRecorder.recordElementsFrom(
        firElement = firDeclaration,
        recorder = FileStructureElement.recorderFor(firDeclaration),
    )

    override fun invoke(element: KtElement): FirElement? {
        return mapping[element]
    }
}

internal class PartialBodyDeclarationFirElementProvider(
    private val declaration: FirDeclaration,
    private val psiDeclaration: KtDeclaration,
    private val psiBlock: KtBlockExpression,
    private val psiStatements: List<KtExpression>,
    private val session: LLFirResolvableModuleSession
) : DeclarationFirElementProvider {
    companion object {
        private val LOG = logger<PartialBodyDeclarationFirElementProvider>()

        private fun createEmptyState(psiStatementCount: Int): LLPartialBodyResolveState {
            return LLPartialBodyResolveState(
                totalPsiStatementCount = psiStatementCount,
                analyzedPsiStatementCount = 0,
                analyzedFirStatementCount = 0,
                performedAnalysesCount = 0,
                analysisStateSnapshot = null,
                previousState = null
            )
        }

        private fun findContainer(
            psiElement: KtElement,
            psiDeclaration: KtDeclaration,
            psiBlock: KtBlockExpression,
            psiStatements: List<KtExpression>,
        ): ElementContainer {
            var previous: PsiElement? = null

            for (current in psiElement.parentsWithSelf) {
                when (current) {
                    psiBlock -> {
                        when (previous) {
                            null -> {
                                // The body block itself is requested.
                                // Here we treat it as a last statement of that block.
                                return ElementContainer.Body(psiStatements.lastIndex)
                            }
                            is KtElement -> {
                                val psiStatementIndex = psiStatements.indexOf(previous)
                                checkWithAttachment(psiStatementIndex >= 0, { "The topmost statement was not found" }) {
                                    withPsiEntry("statement", previous)
                                    withPsiEntry("declaration", psiDeclaration)
                                }
                                return ElementContainer.Body(psiStatementIndex)
                            }
                            else -> break
                        }
                    }
                    is KtParameter -> {
                        val parentParameterList = current.parent as? KtParameterList
                        val parentDeclaration = parentParameterList?.parent

                        // There can be local declarations, destructuring declarations, etc.
                        if (parentDeclaration == psiDeclaration) {
                            return if (previous is KtExpression && current.defaultValue == previous) {
                                ElementContainer.SignatureBody
                            } else {
                                ElementContainer.Signature
                            }
                        }
                    }
                    is KtConstructorDelegationCall -> {
                        if (current.parent == psiDeclaration) {
                            return ElementContainer.SignatureBody
                        }
                    }
                    psiDeclaration -> {
                        return ElementContainer.Signature
                    }
                }

                previous = current
            }

            val error = buildErrorWithAttachment("Cannot find the element container") {
                withPsiEntry("element", psiElement)
                withPsiEntry("declaration", psiDeclaration)
            }

            LOG.error(error)

            return ElementContainer.Unknown
        }
    }

    /**
     * Contains the latest known partial body resolution state.
     *
     * Initially, the [cachedState] is empty, even though the declaration itself may already be partially resolved.
     * On querying the mapping (by calling [invoke]), the actual resolved state is synchronized with the [cachedState],
     * and all missing elements are added to [bodyMappings].
     */
    @Volatile
    private var cachedState: LLPartialBodyResolveState = createEmptyState(psiStatements.size)

    /**
     * Contains mappings for non-body elements.
     */
    private val signatureMappings: Map<KtElement, FirElement> = HashMap<KtElement, FirElement>()
        .also { declaration.accept(DeclarationStructureElement.SignatureRecorder(declaration), it) }

    /**
     * Contains collected mappings.
     * Initially, only signature mappings are registered (the body is ignored).
     * After consequent partial body analysis, elements from analyzed statements are appended.
     */
    @Volatile
    private var bodyMappings: Map<KtElement, FirElement> = emptyMap()

    // The body block cannot be cached on the element provider construction, as the body might be lazy at that point
    private val bodyBlock: FirBlock
        get() = declaration.body ?: error("Partial body element provider supports only declarations with bodies")

    private val lockProvider: LLFirLockProvider
        get() = session.moduleComponents.globalResolveComponents.lockProvider

    override fun invoke(psiElement: KtElement): FirElement? {
        val container = findContainer(psiElement, psiDeclaration, psiBlock, psiStatements)

        val hasBodyFullyResolved = when (container) {
            ElementContainer.Unknown -> return null
            ElementContainer.Signature -> return signatureMappings[psiElement]
            ElementContainer.SignatureBody -> {
                run {
                    // Fast track: the signature body is already analyzed.
                    // Synchronization is not needed here as 'lastState'/'bodyMappings' are addition-only
                    if (cachedState.performedAnalysesCount > 0) {
                        // We performed at least one partial analysis, so we definitely analyzed the parameters
                        return bodyMappings[psiElement]
                    }
                }

                synchronized(this) {
                    // Double-check to avoid more expensive 'performBodyAnalysis()' logic
                    if (cachedState.performedAnalysesCount > 0) {
                        return bodyMappings[psiElement]
                    }
                }

                // We do not need to analyze any statements.
                // However, parameter analysis is performed before body analysis
                performBodyAnalysis(psiStatementLimit = 0)
            }
            is ElementContainer.Body -> {
                val psiStatementLimit = container.psiStatementIndex + 1

                run {
                    // Fast track: required statements are already analyzed.
                    // Synchronization is not needed here as 'lastState'/'bodyMappings' are addition-only
                    val cachedState = this.cachedState
                    if (cachedState.performedAnalysesCount > 0 && cachedState.analyzedPsiStatementCount >= psiStatementLimit) {
                        // The statement is already analyzed and its children are registered
                        return bodyMappings[psiElement]
                    }
                }

                synchronized(this) {
                    // Double-check to avoid more expensive 'performBodyAnalysis()' logic
                    val cachedState = this.cachedState
                    if (cachedState.performedAnalysesCount > 0 && cachedState.analyzedPsiStatementCount >= psiStatementLimit) {
                        return bodyMappings[psiElement]
                    }
                }

                performBodyAnalysis(psiStatementLimit)
            }
        }

        // Process newly analyzed statements serially
        synchronized(this) {
            // It won't help us if some state keepers replace the declaration body with a lazy one in another thread.
            // As long as the declaration hasn't reached the 'BODY_RESOLVE' phase, its body can only be accessed under a lock.
            withDeclarationLock(hasBodyFullyResolved) {
                processResolveStateChanges(psiElement, container)
            }
        }

        return bodyMappings[psiElement]
    }

    private fun processResolveStateChanges(psiElement: PsiElement, container: ElementContainer) {
        val newState = declaration.partialBodyResolveState
        if (newState != null) {
            // Pretend we never analyzed the function if the last state is invalid.
            // In this case, all statements starting from the first one will be re-added to the map.
            val cachedState = this.cachedState

            val lastFirStatementCount = cachedState.analyzedFirStatementCount
            val newFirStatementCount = newState.analyzedFirStatementCount

            val shouldRegisterBodyStatements = newFirStatementCount > lastFirStatementCount
            val shouldRegisterSignatureParts = cachedState.performedAnalysesCount == 0 && declaration is FirFunction

            if (shouldRegisterBodyStatements || shouldRegisterSignatureParts) {
                val consumer = if (cachedState.performedAnalysesCount > 0) HashMap(bodyMappings) else HashMap()

                if (shouldRegisterSignatureParts) {
                    registerDefaultParameterValues(consumer)
                    registerDelegatedConstructorCall(consumer)
                }

                if (shouldRegisterBodyStatements) {
                    val firBody = bodyBlock

                    requireWithAttachment(firBody !is FirLazyBlock, { "Lazy body is unexpected" }) {
                        withFirEntry("fir", declaration)
                        withPsiEntry("declaration", psiDeclaration)
                        withPsiEntry("element", psiElement)
                        withEntry("container", container) { container.toString() }
                    }

                    for (index in lastFirStatementCount until newFirStatementCount) {
                        val firStatement = firBody.statements[index]
                        firStatement.accept(DeclarationStructureElement.Recorder, consumer)
                    }

                    // We can register the block element itself if all its content is analyzed
                    if (newState.analyzedPsiStatementCount == newState.totalPsiStatementCount) {
                        firBody.accept(DeclarationStructureElement.BodyBlockRecorder(firBody), consumer)
                    }
                }

                // Publish new state
                bodyMappings = consumer
                this.cachedState = newState
            }
        } else {
            // The body has never been analyzed (otherwise the partial body resolve state should have been present)
            bodyMappings = HashMap<KtElement, FirElement>()
                .also { consumer ->
                    bodyBlock.accept(DeclarationStructureElement.Recorder, consumer)
                    registerDefaultParameterValues(consumer)
                    registerDelegatedConstructorCall(consumer)
                }
        }
    }

    private fun withDeclarationLock(hasBodyFullyResolved: Boolean, action: () -> Unit) {
        if (hasBodyFullyResolved) {
            // Fast track – the declaration is known to be fully resolved
            action()
        }

        var wasRun = false
        lockProvider.withReadLock(declaration, FirResolvePhase.BODY_RESOLVE) {
            action()
            wasRun = true
        }
        if (!wasRun) {
            // 'withReadLock' does not call the lambda if the declaration already resolved
            action()
        }
    }

    private fun registerDefaultParameterValues(consumer: MutableMap<KtElement, FirElement>) {
        if (declaration is FirFunction) {
            for (parameter in declaration.valueParameters) {
                parameter.defaultValue?.accept(DeclarationStructureElement.Recorder, consumer)
            }
        }
    }

    private fun registerDelegatedConstructorCall(consumer: MutableMap<KtElement, FirElement>) {
        if (declaration is FirConstructor) {
            declaration.delegatedConstructor?.accept(DeclarationStructureElement.Recorder, consumer)
        }
    }

    /**
     * Represents the location of a [PsiElement] for which the FIR mapping was requested.
     */
    sealed class ElementContainer {
        /**
         * The element resides in a declaration signature analysis of which is already complete.
         * [FirResolvePhase.BODY_RESOLVE] is not required to get its mapping.
         */
        data object Signature : ElementContainer()

        /**
         * The element is in parts of the signature that require [FirResolvePhase.BODY_RESOLVE].
         * Examples: default parameter values, delegate constructor call.
         */
        data object SignatureBody : ElementContainer()

        /**
         * The element is inside the declaration body block.
         * [psiStatementIndex] is the index of a topmost block statement which contains the element.
         */
        data class Body(val psiStatementIndex: Int) : ElementContainer()

        /**
         * Some unexpected element.
         */
        data object Unknown : ElementContainer()
    }

    /**
     * Performs partial body analysis up to the [psiStatementLimit] statements.
     * If [psiStatementLimit] is 1, only the first statement is analyzed.
     * If [psiStatementLimit] is 0, statements are not analyzed (but default parameter values are still analyzed).
     *
     * Returns `true` if the whole body is resolved, and the declaration is supposed to be in the [FirResolvePhase.BODY_RESOLVE] phase.
     */
    private fun performBodyAnalysis(psiStatementLimit: Int): Boolean {
        require(psiStatementLimit >= 0)

        if (psiStatementLimit < psiStatements.size) {
            val request = LLPartialBodyResolveRequest(
                target = declaration,
                totalPsiStatementCount = psiStatements.size,
                targetPsiStatementCount = psiStatementLimit,
                stopElement = psiStatements[psiStatementLimit]
            )

            val target = LLFirResolveDesignationCollector.getDesignationToResolveForPartialBody(request)
            if (target != null) {
                session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolveTarget(target, FirResolvePhase.BODY_RESOLVE)
                return false
            }
        }

        declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return true
    }
}

internal val KtDeclaration.bodyBlock: KtBlockExpression?
    get() = when (this) {
        is KtAnonymousInitializer -> body as? KtBlockExpression
        is KtDeclarationWithBody -> bodyBlockExpression
        else -> null
    }