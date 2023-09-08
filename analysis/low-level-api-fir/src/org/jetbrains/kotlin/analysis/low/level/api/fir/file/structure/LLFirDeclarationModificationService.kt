/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService.ModificationType
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

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
        object NewElement : ModificationType()
        object Unknown : ModificationType()
    }

    /**
     * This method should be called during some [PsiElement] modification.
     * This method must be called from write action.
     *
     * Will publish event to [MODULE_OUT_OF_BLOCK_MODIFICATION] in case of out-of-block modification.
     *
     * @param element is an element that we want to modify, remove or add.
     * Some examples:
     * * [element] is [KtNamedFunction][org.jetbrains.kotlin.psi.KtNamedFunction] if we
     * dropped body ([KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression]) of this function
     * * [element] is [KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression] if we replaced one body-expression with another one
     * * [element] is [KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression] if added a body to the function without body
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

    private fun calculateChangeType(element: PsiElement, modificationType: ModificationType): ChangeType = when {
        // If PSI is not valid, well something bad happened; OOBM won't hurt
        !element.isValid -> ChangeType.OutOfBlock
        element is PsiWhiteSpace || element is PsiComment -> ChangeType.Invisible
        // TODO improve for Java KTIJ-21684
        element.language !is KotlinLanguage -> ChangeType.OutOfBlock
        else -> {
            val inBlockModificationOwner = nonLocalDeclarationForLocalChange(element)
            if (inBlockModificationOwner != null && (element.parent != inBlockModificationOwner || modificationType != ModificationType.NewElement)) {
                ChangeType.InBlock(inBlockModificationOwner, project)
            } else {
                ChangeType.OutOfBlock
            }
        }
    }

    private fun inBlockModification(declaration: KtElement, ktModule: KtModule) {
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
