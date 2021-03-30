/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.types.Variance

class RemoveModifierFix(
    element: KtModifierListOwner,
    private val modifier: KtModifierKeywordToken,
    private val isRedundant: Boolean
) : KotlinCrossLanguageQuickFixAction<KtModifierListOwner>(element) {

    private val text = run {
        val modifierText = modifier.value
        when {
            isRedundant ->
                KotlinBundle.message("remove.redundant.0.modifier", modifierText)
            modifier === KtTokens.ABSTRACT_KEYWORD || modifier === KtTokens.OPEN_KEYWORD ->
                KotlinBundle.message("make.0.not.1", getElementName(element), modifierText)
            else ->
                KotlinBundle.message("remove.0.modifier", modifierText, modifier)
        }
    }

    override fun getFamilyName() = KotlinBundle.message("remove.modifier")

    override fun getText() = text

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile) = element?.hasModifier(modifier) == true

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        invoke()
    }

    operator fun invoke() {
        element?.removeModifier(modifier)
    }

    companion object {
        val removeRedundantModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFactory(isRedundant = true)
        val removeNonRedundantModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFactory(isRedundant = false)
        val removeAbstractModifier: QuickFixesPsiBasedFactory<PsiElement> =
            createRemoveModifierFromListOwnerPsiBasedFactory(KtTokens.ABSTRACT_KEYWORD)
        val removeRedundantOpenModifier: QuickFixesPsiBasedFactory<KtModifierListOwner> =
            createRemoveModifierFromListOwnerFactoryByModifierListOwner(
                modifier = KtTokens.OPEN_KEYWORD,
                isRedundant = true
            )
        val removeOpenModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFromListOwnerPsiBasedFactory(KtTokens.OPEN_KEYWORD)
        val removePrivateModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFromListOwnerPsiBasedFactory(KtTokens.PRIVATE_KEYWORD)

        @Deprecated(
            "For binary compatibility",
            replaceWith = ReplaceWith("createRemoveModifierFromListOwnerPsiBasedFactory(modifier, isRedundant)")
        )
        fun createRemoveModifierFromListOwnerFactory(
            modifier: KtModifierKeywordToken,
            isRedundant: Boolean = false
        ): KotlinSingleIntentionActionFactory =
            KotlinSingleIntentionActionFactory.createFromQuickFixesPsiBasedFactory(
                createRemoveModifierFromListOwnerPsiBasedFactory(modifier, isRedundant)
            )

        fun createRemoveModifierFromListOwnerPsiBasedFactory(
            modifier: KtModifierKeywordToken,
            isRedundant: Boolean = false
        ): QuickFixesPsiBasedFactory<PsiElement> =
            createRemoveModifierFromListOwnerFactoryByModifierListOwner(
                modifier,
                isRedundant
            ).coMap { PsiTreeUtil.getParentOfType(it, KtModifierListOwner::class.java, false) }

        fun createRemoveModifierFromListOwnerFactoryByModifierListOwner(
            modifier: KtModifierKeywordToken,
            isRedundant: Boolean = false
        ) = quickFixesPsiBasedFactory<KtModifierListOwner> {
            listOf(RemoveModifierFix(it, modifier, isRedundant))
        }

        private fun createRemoveModifierFactory(isRedundant: Boolean = false): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val elementType = psiElement.node.elementType as? KtModifierKeywordToken ?: return@quickFixesPsiBasedFactory emptyList()
                val modifierListOwner = psiElement.getStrictParentOfType<KtModifierListOwner>()
                    ?: return@quickFixesPsiBasedFactory emptyList()
                listOf(RemoveModifierFix(modifierListOwner, elementType, isRedundant))
            }
        }


        fun createRemoveProjectionFactory(isRedundant: Boolean): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val projection = psiElement as KtTypeProjection
                val elementType = projection.projectionToken?.node?.elementType as? KtModifierKeywordToken
                    ?: return@quickFixesPsiBasedFactory listOf()
                listOf(RemoveModifierFix(projection, elementType, isRedundant))
            }
        }

        fun createRemoveVarianceFactory(): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                require(psiElement is KtTypeParameter)
                val modifier = when (psiElement.variance) {
                    Variance.IN_VARIANCE -> KtTokens.IN_KEYWORD
                    Variance.OUT_VARIANCE -> KtTokens.OUT_KEYWORD
                    else -> return@quickFixesPsiBasedFactory emptyList()
                }
                listOf(RemoveModifierFix(psiElement, modifier, isRedundant = false))
            }
        }

        fun createRemoveSuspendFactory(): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val modifierList = psiElement.parent as KtDeclarationModifierList
                val type = modifierList.parent as KtTypeReference
                if (!type.hasModifier(KtTokens.SUSPEND_KEYWORD)) return@quickFixesPsiBasedFactory emptyList()
                listOf(RemoveModifierFix(type, KtTokens.SUSPEND_KEYWORD, isRedundant = false))
            }
        }

        fun createRemoveLateinitFactory(): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val property = psiElement as? KtProperty ?: return@quickFixesPsiBasedFactory emptyList()
                if (!property.hasModifier(KtTokens.LATEINIT_KEYWORD)) return@quickFixesPsiBasedFactory emptyList()
                listOf(RemoveModifierFix(property, KtTokens.LATEINIT_KEYWORD, isRedundant = false))
            }
        }

        fun createRemoveFunFromInterfaceFactory(): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val modifierList = psiElement.parent as? KtDeclarationModifierList
                    ?: return@quickFixesPsiBasedFactory emptyList()
                val funInterface = (modifierList.parent as? KtClass)?.takeIf {
                    it.isInterface() && it.hasModifier(KtTokens.FUN_KEYWORD)
                } ?: return@quickFixesPsiBasedFactory emptyList()
                listOf(RemoveModifierFix(funInterface, KtTokens.FUN_KEYWORD, isRedundant = false))
            }
        }

        fun getElementName(modifierListOwner: KtModifierListOwner): String {
            var name: String? = null
            if (modifierListOwner is PsiNameIdentifierOwner) {
                val nameIdentifier = modifierListOwner.nameIdentifier
                if (nameIdentifier != null) {
                    name = nameIdentifier.text
                } else if ((modifierListOwner as? KtObjectDeclaration)?.isCompanion() == true) {
                    name = "companion object"
                }
            } else if (modifierListOwner is KtPropertyAccessor) {
                name = modifierListOwner.namePlaceholder.text
            }
            if (name == null) {
                name = modifierListOwner.text
            }
            return "'$name'"
        }
    }
}
