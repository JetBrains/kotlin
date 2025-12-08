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
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.resolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.buildErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.lang.Exception

/**
 * A provider of mapping between [KtElement]s and [FirElement]s.
 */
internal fun interface LLElementMapper : (KtElement) -> FirElement?

/**
 * A provider that collects mapping for the [declaration] right on the provider creation.
 * The [declaration] must be fully analyzed up to the [FirResolvePhase.BODY_RESOLVE].
 */
internal class LLEagerElementMapper(declaration: FirDeclaration) : LLElementMapper {
    private val session = declaration.moduleData.session

    private val mapping = FirElementsRecorder.recordElementsFrom(
        firElement = declaration,
        recorder = FileStructureElement.recorderFor(declaration),
    )

    override fun invoke(element: KtElement): FirElement? {
        return KtToFirMapping.getFir(element, session, mapping)
    }
}

/**
 * A provider for a partially analyzable [declaration].
 * The declaration must be full analyzed up to the [FirResolvePhase.ANNOTATION_ARGUMENTS].
 *
 * @param declaration The declaration to be resolved. Must be [isPartiallyAnalyzable].
 * @param psiDeclaration The PSI version of the [declaration].
 * @param psiBlock The block body of the [psiDeclaration].
 * @param psiStatements All statements from the [psiBlock].
 * @param session The session hosting the [declaration].
 */
internal class LLPartialBodyElementMapper(
    private val declaration: FirDeclaration,
    private val psiDeclaration: KtDeclaration,
    private val psiBlock: KtBlockExpression,
    private val psiStatements: List<KtExpression>,
    private val session: LLFirResolvableModuleSession
) : LLElementMapper {
    init {
        val phase = declaration.resolvePhase
        checkWithAttachment(
            phase >= FirResolvePhase.ANNOTATION_ARGUMENTS,
            { "The declaration must be at least resolved up to ${FirResolvePhase.ANNOTATION_ARGUMENTS.name}, but it is resolved to $phase" },
        )
    }

    companion object {
        private val LOG = logger<LLPartialBodyElementMapper>()

        private fun createEmptyState(psiStatementCount: Int): LLPartialBodyAnalysisState {
            return LLPartialBodyAnalysisState(
                totalPsiStatementCount = psiStatementCount,
                analyzedPsiStatementCount = 0,
                analyzedFirStatementCount = 0,
                performedAnalysesCount = 0,
                analysisStateSnapshot = null
            )
        }

        /**
         * Checks whether the [declaration] can be analyzed partially to get the [element] resolved.
         * The [element] must belong to a [declaration].
         */
        fun isPartiallyAnalyzable(element: KtElement, declaration: KtDeclaration): Boolean {
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
                                    withEntry("statements") {
                                        for ((index, psiStatement) in psiStatements.withIndex()) {
                                            this@withEntry.println(index, ": ", psiStatement.text)
                                        }
                                    }
                                }
                                return ElementContainer.Body(psiStatementIndex)
                            }
                            else -> break
                        }
                    }
                    is KtParameter -> {
                        val parentDeclaration = current.ownerDeclaration

                        // There can be local declarations, lambda destructuring declarations, etc.
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
        get() = declaration.body ?: errorWithFirSpecificEntries(
            "Partial body element provider supports only declarations with bodies",
            fir = declaration,
            psi = psiDeclaration,
        )

    private val lock = Any()

    override fun invoke(psiElement: KtElement): FirElement? {
        val container = try {
            findContainer(psiElement, psiDeclaration, psiBlock, psiStatements)
        } catch (e: Exception) {
            rethrowExceptionWithDetails("Unable to find the element container", e) {
                withEntry("session", session) { it.toString() }
                withFirEntry("fir", declaration)
                withPsiEntry("psiElement", psiElement)
            }
        }

        when (container) {
            ElementContainer.Unknown -> return null
            ElementContainer.Signature -> return KtToFirMapping.getFir(psiElement, session, signatureMappings)
            ElementContainer.SignatureBody -> {
                // Fast track: the signature body is already analyzed.
                // Synchronization is not needed here as 'cachedState'/'bodyMappings' are addition-only
                if (cachedState.performedAnalysesCount > 0) {
                    // We performed at least one partial analysis, so we definitely analyzed the signature
                    return KtToFirMapping.getFir(psiElement, session, bodyMappings)
                }

                // We do not need to analyze any statements.
                // However, parameter analysis is performed before body analysis
                performBodyAnalysis(psiStatementLimit = 0)
            }
            is ElementContainer.Body -> {
                val psiStatementLimit = container.psiStatementIndex + 1

                // Fast track: required statements are already analyzed.
                // Synchronization is not needed here as 'cachedState'/'bodyMappings' are addition-only
                val cachedState = this.cachedState
                if (cachedState.performedAnalysesCount > 0 && cachedState.analyzedPsiStatementCount >= psiStatementLimit) {
                    // The statement is already analyzed and its children are registered
                    return KtToFirMapping.getFir(psiElement, session, bodyMappings)
                }

                performBodyAnalysis(psiStatementLimit)
            }
        }

        val bodyMappings = synchronized(lock) {
            // Process newly analyzed statements serially
            processBodyAnalysisResult()
        }

        return KtToFirMapping.getFir(psiElement, session, bodyMappings)
    }

    private fun processBodyAnalysisResult(): Map<KtElement, FirElement> {
        val existingBodyMappings = this.bodyMappings
        val newState = declaration.partialBodyAnalysisState

        if (newState != null) {
            // Pretend we never analyzed the function if the last state is invalid.
            // In this case, all statements starting from the first one will be re-added to the map.
            val cachedState = this.cachedState

            val lastStatementCount = cachedState.analyzedFirStatementCount
            val newStatementCount = newState.analyzedFirStatementCount

            val shouldRegisterBodyStatements = newStatementCount > lastStatementCount
            val shouldRegisterSignatureParts = cachedState.performedAnalysesCount == 0

            if (shouldRegisterBodyStatements || shouldRegisterSignatureParts) {
                val newBodyMappings = if (cachedState.performedAnalysesCount > 0) HashMap(existingBodyMappings) else HashMap()

                if (shouldRegisterSignatureParts) {
                    registerSignatureBodyParts(newState, newBodyMappings)
                }

                if (shouldRegisterBodyStatements) {
                    val statements = newState.analysisStateSnapshot?.result?.statements ?: bodyBlock.statements

                    for (index in lastStatementCount until newStatementCount) {
                        val statement = statements[index]
                        statement.accept(DeclarationStructureElement.Recorder, newBodyMappings)
                    }

                    // We can register the block element itself if all its content is analyzed
                    if (newState.isFullyAnalyzed) {
                        val bodyBlock = this.bodyBlock
                        bodyBlock.accept(DeclarationStructureElement.BodyBlockRecorder(bodyBlock), newBodyMappings)
                    }
                }

                // Publish new state
                this.bodyMappings = newBodyMappings
                this.cachedState = newState

                return newBodyMappings
            }
        } else {
            // Another thread might have already produced body mappings
            if (existingBodyMappings.isEmpty()) {
                // The body has never been analyzed (otherwise the partial body resolve state should have been present)
                val newBodyMappings = HashMap<KtElement, FirElement>()
                    .also { consumer ->
                        bodyBlock.accept(DeclarationStructureElement.Recorder, consumer)
                        registerSignatureBodyParts(newState = null, consumer)
                    }

                this.bodyMappings = newBodyMappings
                return newBodyMappings
            }
        }

        return existingBodyMappings
    }

    private fun registerSignatureBodyParts(newState: LLPartialBodyAnalysisState?, consumer: MutableMap<KtElement, FirElement>) {
        registerDefaultParameterValues(newState, consumer)
        registerDelegatedConstructorCall(newState, consumer)
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
                target.resolve(FirResolvePhase.BODY_RESOLVE)
                return
            }
        }

        declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    }
}

/**
 * A [KtBlockExpression] body for a callable declaration.
 */
internal val KtDeclaration.bodyBlock: KtBlockExpression?
    get() = when (this) {
        is KtAnonymousInitializer -> body as? KtBlockExpression
        is KtDeclarationWithBody -> bodyBlockExpression
        else -> null
    }