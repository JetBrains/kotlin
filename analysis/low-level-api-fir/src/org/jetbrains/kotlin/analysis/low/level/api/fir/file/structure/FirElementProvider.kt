/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveRequest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyAnalysisState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.partialBodyAnalysisState
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.buildErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
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

        private fun createEmptyState(psiStatementCount: Int): LLPartialBodyAnalysisState {
            return LLPartialBodyAnalysisState(
                totalPsiStatementCount = psiStatementCount,
                analyzedPsiStatementCount = 0,
                analyzedFirStatementCount = 0,
                performedAnalysesCount = 0,
                analysisStateSnapshot = null,
                previousState = null
            )
        }

        fun isPartiallyResolvable(element: KtElement, declaration: KtDeclaration): Boolean {
            val block = declaration.bodyBlock ?: return false
            val container = findContainer(element, declaration, block, block.statements)
            return when (container) {
                is ElementContainer.Body, ElementContainer.SignatureBody -> true
                else -> false
            }
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
    private var cachedState: LLPartialBodyAnalysisState = createEmptyState(psiStatements.size)

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

    override fun invoke(psiElement: KtElement): FirElement? {
        val container = findContainer(psiElement, psiDeclaration, psiBlock, psiStatements)

        when (container) {
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

        synchronized(this) {
            // Process newly analyzed statements serially
            processBodyAnalysisResult()
        }

        return bodyMappings[psiElement]
    }

    private fun processBodyAnalysisResult() {
        val newState = declaration.partialBodyAnalysisState
        if (newState != null) {
            // Pretend we never analyzed the function if the last state is invalid.
            // In this case, all statements starting from the first one will be re-added to the map.
            val cachedState = this.cachedState

            val lastStatementCount = cachedState.analyzedFirStatementCount
            val newStatementCount = newState.analyzedFirStatementCount

            val shouldRegisterBodyStatements = newStatementCount > lastStatementCount
            val shouldRegisterSignatureParts = cachedState.performedAnalysesCount == 0 && declaration is FirFunction

            if (shouldRegisterBodyStatements || shouldRegisterSignatureParts) {
                val consumer = if (cachedState.performedAnalysesCount > 0) HashMap(bodyMappings) else HashMap()

                if (shouldRegisterSignatureParts) {
                    registerDefaultParameterValues(newState, consumer)
                    registerDelegatedConstructorCall(newState, consumer)
                }

                if (shouldRegisterBodyStatements) {
                    val statements = newState.analysisStateSnapshot?.result?.statements ?: bodyBlock.statements

                    for (index in lastStatementCount until newStatementCount) {
                        val statement = statements[index]
                        statement.accept(DeclarationStructureElement.Recorder, consumer)
                    }

                    // We can register the block element itself if all its content is analyzed
                    if (newState.analyzedPsiStatementCount == newState.totalPsiStatementCount) {
                        val bodyBlock = this.bodyBlock
                        bodyBlock.accept(DeclarationStructureElement.BodyBlockRecorder(bodyBlock), consumer)
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
                    registerDefaultParameterValues(newState = null, consumer)
                    registerDelegatedConstructorCall(newState = null, consumer)
                }
        }
    }

    private fun registerDefaultParameterValues(newState: LLPartialBodyAnalysisState?, consumer: MutableMap<KtElement, FirElement>) {
        val snapshot = newState?.analysisStateSnapshot
        if (snapshot != null) {
            for (defaultValue in snapshot.result.defaultParameterValues) {
                defaultValue.accept(DeclarationStructureElement.Recorder, consumer)
            }
            return
        }

        if (declaration is FirFunction) {
            for (parameter in declaration.valueParameters) {
                parameter.defaultValue?.accept(DeclarationStructureElement.Recorder, consumer)
            }
        }
    }

    private fun registerDelegatedConstructorCall(newState: LLPartialBodyAnalysisState?, consumer: MutableMap<KtElement, FirElement>) {
        val snapshot = newState?.analysisStateSnapshot
        if (snapshot != null) {
            snapshot.result.delegatedConstructorCall?.accept(DeclarationStructureElement.Recorder, consumer)
            return
        }

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
     */
    private fun performBodyAnalysis(psiStatementLimit: Int) {
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
                return
            }
        }

        declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    }
}

internal val KtDeclaration.bodyBlock: KtBlockExpression?
    get() = when (this) {
        is KtAnonymousInitializer -> body as? KtBlockExpression
        is KtDeclarationWithBody -> bodyBlockExpression
        else -> null
    }