/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.inspections.KotlinUniversalQuickFix
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

open class AddModifierFix(
    element: KtModifierListOwner,
    protected val modifier: KtModifierKeywordToken
) : KotlinCrossLanguageQuickFixAction<KtModifierListOwner>(element), KotlinUniversalQuickFix {
    override fun getText(): String {
        val element = element ?: return ""
        if (modifier in modalityModifiers || modifier in KtTokens.VISIBILITY_MODIFIERS || modifier == KtTokens.CONST_KEYWORD) {
            return KotlinBundle.message("fix.add.modifier.text", RemoveModifierFix.getElementName(element), modifier.value)
        }
        return KotlinBundle.message("fix.add.modifier.text.generic", modifier.value)
    }

    override fun getFamilyName() = KotlinBundle.message("fix.add.modifier.family")

    protected fun invokeOnElement(element: KtModifierListOwner?) {
        element?.addModifier(modifier)

        if (modifier == KtTokens.ABSTRACT_KEYWORD && (element is KtProperty || element is KtNamedFunction)) {
            element.containingClass()?.run {
                if (!hasModifier(KtTokens.ABSTRACT_KEYWORD) && !hasModifier(KtTokens.SEALED_KEYWORD)) {
                    addModifier(KtTokens.ABSTRACT_KEYWORD)
                }
            }
        }
    }

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        val originalElement = element
        invokeOnElement(originalElement)
    }

    // TODO: consider checking if this fix is available by testing if the [element] can be refactored by calling
    //  FIR version of [org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtilKt#canRefactor]
    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean = element != null

    interface Factory<T> {
        fun createFactory(modifier: KtModifierKeywordToken): QuickFixesPsiBasedFactory<PsiElement> {
            return createFactory(modifier, KtModifierListOwner::class.java)
        }

        fun <T : KtModifierListOwner> createFactory(
            modifier: KtModifierKeywordToken,
            modifierOwnerClass: Class<T>
        ): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { e ->
                val modifierListOwner =
                    PsiTreeUtil.getParentOfType(e, modifierOwnerClass, false) ?: return@quickFixesPsiBasedFactory emptyList()
                listOfNotNull(createIfApplicable(modifierListOwner, modifier))
            }
        }

        fun createIfApplicable(modifierListOwner: KtModifierListOwner, modifier: KtModifierKeywordToken): AddModifierFix? {
            when (modifier) {
                KtTokens.ABSTRACT_KEYWORD, KtTokens.OPEN_KEYWORD -> {
                    if (modifierListOwner is KtObjectDeclaration) return null
                    if (modifierListOwner is KtEnumEntry) return null
                    if (modifierListOwner is KtDeclaration && modifierListOwner !is KtClass) {
                        val parentClassOrObject = modifierListOwner.containingClassOrObject ?: return null
                        if (parentClassOrObject is KtObjectDeclaration) return null
                        if (parentClassOrObject is KtEnumEntry) return null
                    }
                    if (modifier == KtTokens.ABSTRACT_KEYWORD
                        && modifierListOwner is KtClass
                        && modifierListOwner.hasModifier(KtTokens.INLINE_KEYWORD)
                    ) return null
                }
                KtTokens.INNER_KEYWORD -> {
                    if (modifierListOwner is KtObjectDeclaration) return null
                    if (modifierListOwner is KtClass) {
                        if (modifierListOwner.isInterface() ||
                            modifierListOwner.isSealed() ||
                            modifierListOwner.isEnum() ||
                            modifierListOwner.isData() ||
                            modifierListOwner.isAnnotation()
                        ) return null
                    }
                }
            }
            return AddModifierFix(modifierListOwner, modifier)
        }

        fun createModifierFix(
            element: KtModifierListOwner,
            modifier: KtModifierKeywordToken
        ): T
    }

    companion object : Factory<AddModifierFix> {
        val addAbstractModifier = AddModifierFix.createFactory(KtTokens.ABSTRACT_KEYWORD)
        val addAbstractToContainingClass = AddModifierFix.createFactory(KtTokens.ABSTRACT_KEYWORD, KtClassOrObject::class.java)
        val addOpenToContainingClass = AddModifierFix.createFactory(KtTokens.OPEN_KEYWORD, KtClassOrObject::class.java)
        val addFinalToProperty = AddModifierFix.createFactory(KtTokens.FINAL_KEYWORD, KtProperty::class.java)
        val addInnerModifier = createFactory(KtTokens.INNER_KEYWORD)

        private val modalityModifiers: Set<KtModifierKeywordToken> =
            setOf(KtTokens.ABSTRACT_KEYWORD, KtTokens.OPEN_KEYWORD, KtTokens.FINAL_KEYWORD)

        override fun createModifierFix(element: KtModifierListOwner, modifier: KtModifierKeywordToken): AddModifierFix =
            AddModifierFix(element, modifier)

    }
}
