/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService.ModificationType
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics.CODE_FRAGMENT_CONTEXT_MODIFICATION
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isContractDescriptionCallPsiCheck

/**
 * This service is responsible for processing incoming [PsiElement] changes to reflect them on FIR tree.
 *
 * For local changes (in-block modification), this service will do all required work.
 *
 * In case of non-local changes (out-of-block modification), this service will publish event to [MODULE_OUT_OF_BLOCK_MODIFICATION].
 *
 * @see MODULE_OUT_OF_BLOCK_MODIFICATION
 * @see org.jetbrains.kotlin.analysis.providers.topics.KotlinModuleOutOfBlockModificationListener
 */
@LLFirInternals
class LLFirDeclarationModificationService(val project: Project) : Disposable {
    init {
        ApplicationManager.getApplication().addApplicationListener(
            object : ApplicationListener {
                override fun writeActionFinished(action: Any) {
                    flushModifications()
                }
            },
            this,
        )
    }

    private var inBlockModificationQueue: MutableSet<ChangeType.InBlock>? = null

    private fun addModificationToQueue(modification: ChangeType.InBlock) {
        // There is no sense to add into the queue elements with unresolved body
        if (!modification.blockOwner.hasFirBody) return

        val queue = inBlockModificationQueue ?: HashSet<ChangeType.InBlock>().also { inBlockModificationQueue = it }
        queue += modification
    }

    /**
     * We can avoid processing of in-block modification with the same [KtModule] because they
     * will be invalidated anyway by OOBM
     */
    private fun dropOutdatedModifications(ktModuleWithOutOfBlockModification: KtModule) {
        processQueue { value, iterator ->
            if (value.ktModule == ktModuleWithOutOfBlockModification) iterator.remove()
        }
    }

    /**
     * Process valid elements in the current queue.
     * Non-valid elements will be dropped from the queue during this iteration.
     *
     * @param action will be executed for each valid element in the queue;
     * **value** is a current element;
     * **iterator** is the corresponding iterator for this element.
     */
    private inline fun processQueue(action: (value: ChangeType.InBlock, iterator: MutableIterator<ChangeType.InBlock>) -> Unit) {
        val queue = inBlockModificationQueue ?: return
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (!element.blockOwner.isValid) {
                iterator.remove()
                continue
            }

            action(element, iterator)
        }
    }

    /**
     * Force the service to publish delayed modifications. This action is required to fix inconsistencies in FirFile tree.
     */
    fun flushModifications() {
        ApplicationManager.getApplication().assertIsWriteThread()

        processQueue { value, _ ->
            inBlockModification(value.blockOwner, value.ktModule)
        }

        inBlockModificationQueue = null
    }

    sealed class ModificationType {
        /**
         * The element passed to [elementModified] has been added as a new element.
         */
        object ElementAdded : ModificationType()

        /**
         * The element passed to [elementModified] is the parent of a removed element, which is additionally passed via the modification
         * type as [removedElement]. The removed element itself cannot be the modification "anchor" because it has already been removed and
         * is not part of the `KtFile` anymore, but it might still be used to determine the modification's change type.
         */
        class ElementRemoved(val removedElement: PsiElement) : ModificationType()

        object Unknown : ModificationType()
    }

    /**
     * This method should be called during some [PsiElement] modification.
     * This method must be called from write action.
     *
     * Will publish event to [MODULE_OUT_OF_BLOCK_MODIFICATION] in case of out-of-block modification.
     *
     * @param element is an element that we want to/did already modify, remove, or add.
     * Some examples:
     * * [element] is [KtNamedFunction][org.jetbrains.kotlin.psi.KtNamedFunction] if we
     * dropped body ([KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression]) of this function
     * * [element] is [KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression] if we replaced one body-expression with another one
     * * [element] is [KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression] if added a body to the function without body
     * * [element] is the parent of an already removed element, while [ModificationType.ElementRemoved] will contain the removed element
     *
     * @param modificationType additional information to make more accurate decisions
     */
    fun elementModified(element: PsiElement, modificationType: ModificationType = ModificationType.Unknown) {
        ApplicationManager.getApplication().assertIsWriteThread()

        when (val changeType = calculateChangeType(element, modificationType)) {
            is ChangeType.Invisible -> {}
            is ChangeType.InBlock -> addModificationToQueue(changeType)
            is ChangeType.OutOfBlock -> outOfBlockModification(element)
        }
    }

    private fun calculateChangeType(element: PsiElement, modificationType: ModificationType): ChangeType {
        if (!element.isValid) {
            // If PSI is not valid, well something bad happened; OOBM won't hurt
            return ChangeType.OutOfBlock
        }

        if (element is PsiWhiteSpace || element is PsiComment) {
            return ChangeType.Invisible
        }

        if (element.language !is KotlinLanguage) {
            // TODO improve for Java KTIJ-21684
            return ChangeType.OutOfBlock
        }

        val inBlockModificationOwner = nonLocalDeclarationForLocalChange(element) ?: return ChangeType.OutOfBlock

        if (inBlockModificationOwner is KtCodeFragment) {
            // All code fragment content is local
            return ChangeType.InBlock(inBlockModificationOwner, project)
        }

        val isOutOfBlockChange = element.isNewDirectChildOf(inBlockModificationOwner, modificationType)
                || modificationType.isContractRemoval()

        return when {
            !isOutOfBlockChange -> ChangeType.InBlock(inBlockModificationOwner, project)
            else -> ChangeType.OutOfBlock
        }
    }

    /**
     * This check covers cases such as a new body that was added to a function, which should cause an out-of-block modification.
     */
    private fun PsiElement.isNewDirectChildOf(inBlockModificationOwner: KtAnnotated, modificationType: ModificationType): Boolean =
        modificationType == ModificationType.ElementAdded && parent == inBlockModificationOwner

    /**
     * Contract changes are always out-of-block modifications. If a contract is removed all at once, e.g. via [PsiElement.delete],
     * [isElementInsideBody] will not see the removed contract inside the PSI *after* removal and treat the change as an in-block
     * modification. "Before removal" events aren't necessarily paired up with "after removal" events in the IDE, so we cannot rely on the
     * presence of the contract statement in some "before removal" event.
     *
     * [isContractRemoval] has to analyze the removed element out of context, as it has already been removed from its parent PSI. There
     * might occasionally be false positives, for example removing the contract statement from:
     *
     * ```
     * if (condition) {
     *     contract { ... }
     * }
     * ```
     *
     * As it is not a valid contract statement, its removal doesn't need to trigger an out-of-block modification. Nonetheless, as such a
     * situation should not occur frequently, false positives are acceptable and this simplifies the analysis, making it less error-prone.
     */
    private fun ModificationType.isContractRemoval(): Boolean =
        this is ModificationType.ElementRemoved && (removedElement as? KtExpression)?.isContractDescriptionCallPsiCheck() == true

    private fun inBlockModification(declaration: KtAnnotated, ktModule: KtModule) {
        val resolveSession = ktModule.getFirResolveSession(project)
        val firDeclaration = when (declaration) {
            is KtCodeFragment -> declaration.getOrBuildFirFile(resolveSession).codeFragment
            is KtDeclaration -> declaration.resolveToFirSymbol(resolveSession).fir
            else -> errorWithFirSpecificEntries(
                "Unexpected declaration kind: ${declaration::class.simpleName}",
                psi = declaration,
            )
        }

        invalidateAfterInBlockModification(firDeclaration)
        declaration.hasFirBody = false

        val moduleSession = firDeclaration.llFirResolvableSession ?: errorWithFirSpecificEntries(
            "${LLFirResolvableModuleSession::class.simpleName} is not found",
            fir = firDeclaration,
            psi = declaration,
        ) {
            withEntry("session", resolveSession) { it.toString() }
        }

        val fileStructure = moduleSession.moduleComponents
            .fileStructureCache
            .getCachedFileStructure(declaration.containingKtFile)
            ?: return // we do not have a cache for this file

        fileStructure.invalidateElement(declaration)

        project.analysisMessageBus.syncPublisher(CODE_FRAGMENT_CONTEXT_MODIFICATION).onModification(ktModule)
    }

    private fun outOfBlockModification(element: PsiElement) {
        val ktModule = ProjectStructureProvider.getModule(project, element, contextualModule = null)

        // We should check outdated modifications before to avoid cache dropping (e.g., KtModule cache)
        dropOutdatedModifications(ktModule)
        project.analysisMessageBus.syncPublisher(MODULE_OUT_OF_BLOCK_MODIFICATION).onModification(ktModule)
    }

    /**
     * @return the psi element (ancestor of the changedElement) which should be re-highlighted in case of in-block changes or null if unsure
     */
    fun elementToRehighlight(changedElement: PsiElement): PsiElement? {
        return nonLocalDeclarationForLocalChange(changedElement)
    }

    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): LLFirDeclarationModificationService =
            project.getService(LLFirDeclarationModificationService::class.java)

        /**
         * This function have to be called from Low Level FIR body transformers.
         * It is fine to have false-positives, but false-negatives are not acceptable.
         */
        internal fun bodyResolved(element: FirElementWithResolveState, phase: FirResolvePhase) {
            when (element) {
                is FirSimpleFunction -> {
                    // in-block modifications only applicable to functions with an explicit type,
                    // so we mark only fully resolved functions
                    if (phase != FirResolvePhase.BODY_RESOLVE) return
                }

                is FirProperty -> {
                    // in-block modifications only applicable to properties with an explicit type,
                    // but existed backing field can lead to the entire body resolution even on
                    // implicit body phase, so we will mark this phase as fully resolved too to be safe
                    if (phase != FirResolvePhase.BODY_RESOLVE && phase != FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) return
                }

                is FirCodeFragment -> {
                    // in-block modifications only applicable to fully resolved code fragments
                    if (phase != FirResolvePhase.BODY_RESOLVE) return
                }

                else -> return
            }

            val declaration = element.source?.psi as? KtAnnotated ?: return
            when (declaration) {
                is KtNamedFunction -> {
                    if (declaration.isReanalyzableContainer()) {
                        declaration.hasFirBody = true
                    }
                }

                is KtProperty -> {
                    if (declaration.isReanalyzableContainer() || declaration.accessors.any(KtPropertyAccessor::isReanalyzableContainer)) {
                        declaration.hasFirBody = true
                    }
                }

                is KtCodeFragment -> {
                    declaration.hasFirBody = true
                }
            }
        }
    }
}

private fun nonLocalDeclarationForLocalChange(psi: PsiElement): KtAnnotated? {
    return psi.getNonLocalReanalyzableContainingDeclaration() ?: psi.containingFile as? KtCodeFragment
}

private sealed class ChangeType {
    object OutOfBlock : ChangeType()
    object Invisible : ChangeType()

    class InBlock(val blockOwner: KtAnnotated, val project: Project) : ChangeType() {
        val ktModule: KtModule by lazy(LazyThreadSafetyMode.NONE) {
            ProjectStructureProvider.getModule(project, blockOwner, contextualModule = null)
        }

        override fun equals(other: Any?): Boolean = other === this || other is InBlock && other.blockOwner == blockOwner
        override fun hashCode(): Int = blockOwner.hashCode()
    }
}

/**
 * The purpose of this property as user data is to avoid FIR building in case the [KtAnnotated]
 * doesn't have an associated FIR with body.
 *
 * [KtProperty] is used as an anchor for [KtPropertyAccessor]s to avoid extra memory consumption.
 */
private var KtAnnotated.hasFirBody: Boolean
    get() = when (this) {
        is KtNamedFunction, is KtProperty, is KtCodeFragment -> getUserData(hasFirBodyKey) == true
        is KtPropertyAccessor -> property.hasFirBody
        else -> false
    }
    set(value) {
        val declarationAnchor = if (this is KtPropertyAccessor) property else this
        declarationAnchor.putUserData(
            hasFirBodyKey,
            value.takeIf { it },
        )
    }

private val hasFirBodyKey = Key.create<Boolean?>("HAS_FIR_BODY")

/**
 * Covered by org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractInBlockModificationTest
 * on the compiler side and by
 * org.jetbrains.kotlin.idea.fir.analysis.providers.trackers.AbstractProjectWideOutOfBlockKotlinModificationTrackerTest
 * on the plugin part
 *
 * @return The declaration in which a change of the passed receiver parameter can be treated as in-block modification
 */
internal fun PsiElement.getNonLocalReanalyzableContainingDeclaration(): KtDeclaration? {
    return when (val declaration = getNonLocalContainingOrThisDeclaration()) {
        is KtNamedFunction -> declaration.takeIf { function ->
            function.isReanalyzableContainer() && isElementInsideBody(declaration = function, child = this)
        }

        is KtPropertyAccessor -> declaration.takeIf { accessor ->
            accessor.isReanalyzableContainer() && isElementInsideBody(declaration = accessor, child = this)
        }

        is KtProperty -> declaration.takeIf { property ->
            property.isReanalyzableContainer() && property.delegateExpressionOrInitializer?.isAncestor(this) == true
        }

        else -> null
    }
}

private fun isElementInsideBody(declaration: KtDeclarationWithBody, child: PsiElement): Boolean {
    val body = declaration.bodyExpression ?: return false
    if (!body.isAncestor(child)) return false
    return !isInsideContract(body = body, child = child)
}

private fun isInsideContract(body: KtExpression, child: PsiElement): Boolean {
    if (body !is KtBlockExpression) return false

    val firstStatement = body.firstStatement ?: return false
    if (!firstStatement.isContractDescriptionCallPsiCheck()) return false
    return firstStatement.isAncestor(child)
}

private fun KtNamedFunction.isReanalyzableContainer(): Boolean = hasBlockBody() || typeReference != null

private fun KtPropertyAccessor.isReanalyzableContainer(): Boolean = isSetter || hasBlockBody() || property.typeReference != null

private fun KtProperty.isReanalyzableContainer(): Boolean = typeReference != null && !hasDelegateExpressionOrInitializer()
