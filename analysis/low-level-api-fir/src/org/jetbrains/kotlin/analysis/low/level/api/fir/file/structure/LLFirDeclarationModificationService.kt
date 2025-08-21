/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType
import org.jetbrains.kotlin.analysis.api.platform.modification.publishModuleOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isContractDescriptionCallPsiCheck

/**
 * This service is responsible for processing incoming [PsiElement] changes to reflect them on FIR tree.
 *
 * For local changes (in-block modification), this service will do all required work
 * and publish [LLFirDeclarationModificationTopics.IN_BLOCK_MODIFICATION].
 *
 * In case of non-local changes (out-of-block modification), this service will publish a [KotlinModuleOutOfBlockModificationEvent][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent].
 *
 * @see getNonLocalReanalyzableContainingDeclaration
 * @see org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent
 * @see LLFirDeclarationModificationTopics.IN_BLOCK_MODIFICATION
 * @see org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService
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

        project.messageBus.connect(this).subscribe(
            KtCodeFragment.IMPORT_MODIFICATION,
            KotlinCodeFragmentImportModificationListener { codeFragment -> outOfBlockModification(codeFragment) }
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
     * We can avoid processing of in-block modification with the same [KaModule] because they
     * will be invalidated anyway by OOBM
     */
    private fun dropOutdatedModifications(ktModuleWithOutOfBlockModification: KaModule) {
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
            val owner = element.blockOwner
            if (!owner.isValid || (owner.containingFile as? KtCodeFragment)?.context?.isValid == false) {
                iterator.remove()
                continue
            }

            action(element, iterator)
        }
    }

    /**
     * Force the service to publish delayed modifications.
     * This action is required to fix inconsistencies in [FirFile][org.jetbrains.kotlin.fir.declarations.FirFile] tree.
     */
    fun flushModifications() {
        ApplicationManager.getApplication().assertWriteIntentLockAcquired()

        processQueue { value, _ ->
            inBlockModification(value.blockOwner, value.ktModule)
        }

        inBlockModificationQueue = null
    }

    /**
     * @see org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService.handleElementModification
     */
    fun elementModified(element: PsiElement, modificationType: KaElementModificationType) {
        ApplicationManager.getApplication().assertWriteIntentLockAcquired()

        when (val changeType = calculateChangeType(element, modificationType)) {
            is ChangeType.Invisible -> {}
            is ChangeType.InBlock -> addModificationToQueue(changeType)
            is ChangeType.OutOfBlock -> outOfBlockModification(element)
        }
    }

    private fun calculateChangeType(element: PsiElement, modificationType: KaElementModificationType): ChangeType {
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
                || modificationType.isBackingFieldAccessChange(inBlockModificationOwner)

        return when {
            !isOutOfBlockChange -> ChangeType.InBlock(inBlockModificationOwner, project)
            else -> ChangeType.OutOfBlock
        }
    }

    /**
     * This check covers cases such as a new body that was added to a function, which should cause an out-of-block modification.
     */
    private fun PsiElement.isNewDirectChildOf(inBlockModificationOwner: KtAnnotated, modificationType: KaElementModificationType): Boolean =
        modificationType == KaElementModificationType.ElementAdded && parent == inBlockModificationOwner

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
    private fun KaElementModificationType.isContractRemoval(): Boolean =
        this is KaElementModificationType.ElementRemoved && (removedElement as? KtExpression)?.isContractDescriptionCallPsiCheck() == true

    /**
     * Backing field access changes are always out-of-block modifications.
     *
     * @see potentiallyAffectsPropertyBackingFieldResolution
     */
    private fun KaElementModificationType.isBackingFieldAccessChange(inBlockModificationOwner: KtAnnotated): Boolean =
        inBlockModificationOwner is KtPropertyAccessor &&
                this is KaElementModificationType.ElementRemoved &&
                removedElement.potentiallyAffectsPropertyBackingFieldResolution()

    private fun inBlockModification(declaration: KtAnnotated, module: KaModule) {
        val resolutionFacade = module.getResolutionFacade(project)
        val firDeclaration = when (declaration) {
            is KtCodeFragment -> declaration.getOrBuildFirFile(resolutionFacade).codeFragment
            is KtDeclaration -> declaration.resolveToFirSymbol(resolutionFacade).fir
            else -> errorWithFirSpecificEntries(
                "Unexpected declaration kind: ${declaration::class.simpleName}",
                psi = declaration,
            )
        }

        // 1. Invalidate FIR
        invalidateAfterInBlockModification(firDeclaration)
        declaration.hasFirBody = false

        val moduleSession = firDeclaration.llFirResolvableSession ?: errorWithFirSpecificEntries(
            "${LLFirResolvableModuleSession::class.simpleName} is not found",
            fir = firDeclaration,
            psi = declaration,
        ) {
            withEntry("session", resolutionFacade) { it.toString() }
        }

        // 2. Invalidate caches
        moduleSession.moduleComponents
            .fileStructureCache
            .getCachedFileStructure(declaration.containingKtFile)
            ?.invalidateElement(declaration)

        // 3. Publish event
        project.analysisMessageBus
            .syncPublisher(LLFirDeclarationModificationTopics.IN_BLOCK_MODIFICATION)
            .afterModification(declaration, module)
    }

    private fun outOfBlockModification(element: PsiElement) {
        val module = KotlinProjectStructureProvider.getModule(project, element, useSiteModule = null)

        // We should check outdated modifications before to avoid cache dropping (e.g., KaModule cache)
        dropOutdatedModifications(module)
        module.publishModuleOutOfBlockModificationEvent()
    }

    /**
     * @see org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService.ancestorAffectedByInBlockModification
     */
    fun ancestorAffectedByInBlockModification(changedElement: PsiElement): PsiElement? = nonLocalDeclarationForLocalChange(changedElement)

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
        val ktModule: KaModule by lazy(LazyThreadSafetyMode.NONE) {
            KotlinProjectStructureProvider.getModule(project, blockOwner, useSiteModule = null)
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
            function.isReanalyzableContainer() && isElementInsideBody(
                declaration = function,
                child = this,
                canHaveBackingFieldAccess = false,
            )
        }

        is KtPropertyAccessor -> declaration.takeIf { accessor ->
            accessor.isReanalyzableContainer() && isElementInsideBody(
                declaration = accessor,
                child = this,
                canHaveBackingFieldAccess = true,
            )
        }

        is KtProperty -> declaration.takeIf { property ->
            property.isReanalyzableContainer() && property.delegateExpressionOrInitializer?.isAncestor(this) == true
        }

        else -> null
    }
}

/**
 * # Regular access
 *
 * ```kotlin
 * val i: Int
 *   get() {
 *     field // Depending on the existence of this access, the property will have or not the backing field
 *     return 0
 *   }
 * ```
 *
 * # Leading local declaration
 *
 * ```kotlin
 * val i: Int
 *   get() {
 *     // Also, we cannot just ignore such local declarations existence as they may change the resolution
 *     // of backing field. With this declaration,
 *     // the next `field` access will be resolved into this local property,
 *     // so there will be no any access to the backing field and,
 *     // as the result, there will be no backing field at all
 *     val field = 1
 *     field
 *     return 0
 *   }
 * ```
 *
 * # Implicit receiver
 *
 * ```kotlin
 * class MyClass(val field: String)
 * fun action(block: () -> Unit) {}
 * fun actionWithReceiver(block: MyClass.() -> Unit) {}
 *
 * val prop: Int
 *   get() {
 *     // Here we can safely change `action` to `actionWithReceiver` and vise versa
 *     // as `field` in both cases will be resolved into the backing field
 *     // as it has higher priority than a property from an implicit receiver
 *     action {
 *       field
 *     }
 *
 *     return 0
 *   }
 * ```
 */
private fun PsiElement.potentiallyAffectsPropertyBackingFieldResolution(): Boolean {
    var hasFieldText = false
    this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is LeafPsiElement && element.textMatches(StandardNames.BACKING_FIELD.asString())) {
                hasFieldText = true
                stopWalking()
            } else {
                super.visitElement(element)
            }
        }
    })

    return hasFieldText
}

private fun isElementInsideBody(declaration: KtDeclarationWithBody, child: PsiElement, canHaveBackingFieldAccess: Boolean): Boolean {
    val body = declaration.bodyExpression ?: return false
    return when {
        !body.isAncestor(child) -> false
        isInsideContract(body = body, child = child) -> false
        canHaveBackingFieldAccess && child.potentiallyAffectsPropertyBackingFieldResolution() -> false
        else -> true
    }
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
