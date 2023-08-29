/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService.ModificationType
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCodeFragment

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
class LLFirDeclarationModificationService(val project: Project) {
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
            is ChangeType.InBlock -> invalidateAfterInBlockModification(changeType.blockOwner)
            is ChangeType.OutOfBlock -> {
                val ktModule = ProjectStructureProvider.getModule(project, element, contextualModule = null)
                project.analysisMessageBus.syncPublisher(MODULE_OUT_OF_BLOCK_MODIFICATION).onModification(ktModule)
            }
        }
    }

    /**
     * @return the psi element (ancestor of the changedElement) which should be re-highlighted in case of in-block changes or null if unsure
     */
    fun elementToRehighlight(changedElement: PsiElement): PsiElement? {
        return nonLocalDeclarationForLocalChange(changedElement)
    }

    companion object {
        fun getInstance(project: Project): LLFirDeclarationModificationService =
            project.getService(LLFirDeclarationModificationService::class.java)
    }
}

private fun calculateChangeType(element: PsiElement, modificationType: ModificationType): ChangeType = when {
    // If PSI is not valid, well something bad happened, OOBM won't hurt
    !element.isValid -> ChangeType.OutOfBlock
    element is PsiWhiteSpace || element is PsiComment -> ChangeType.Invisible
    // TODO improve for Java KTIJ-21684
    element.language !is KotlinLanguage -> ChangeType.OutOfBlock
    else -> {
        val inBlockModificationOwner = nonLocalDeclarationForLocalChange(element)
        if (inBlockModificationOwner != null && (element.parent != inBlockModificationOwner || modificationType != ModificationType.NewElement)) {
            ChangeType.InBlock(inBlockModificationOwner)
        } else {
            ChangeType.OutOfBlock
        }
    }
}

private fun nonLocalDeclarationForLocalChange(psi: PsiElement): KtAnnotated? {
    return psi.getNonLocalReanalyzableContainingDeclaration() ?: psi.containingFile as? KtCodeFragment
}

private sealed class ChangeType {
    object OutOfBlock : ChangeType()
    object Invisible : ChangeType()
    class InBlock(val blockOwner: KtAnnotated) : ChangeType()
}
