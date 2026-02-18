/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub

/**
 * Represents a primary constructor explicitly declared in a class header.
 *
 * ### Example:
 *
 * ```kotlin
 * class Person constructor(val name: String)
 * //           ^___________________________^
 * ```
 */
class KtPrimaryConstructor : KtConstructor<KtPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinConstructorStub<KtPrimaryConstructor>) : super(stub, KtStubBasedElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingClassOrObject() = parent as KtClassOrObject

    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getConstructorKeyword() ?: addBefore(KtPsiFactory(project).createConstructorKeyword(), valueParameterList!!)
    }

    fun removeRedundantConstructorKeywordAndSpace() {
        getConstructorKeyword()?.delete()
        if (prevSibling is PsiWhiteSpace) {
            prevSibling.delete()
        }
    }

    override fun addModifier(modifier: KtModifierKeywordToken) {
        val modifierList = modifierList
        if (modifierList != null) {
            addModifier(modifierList, modifier)
            if (this.modifierList == null) {
                getConstructorKeyword()?.delete()
            }
        } else {
            if (modifier == KtTokens.PUBLIC_KEYWORD) return
            val newModifierList = KtPsiFactory(project).createModifierList(modifier)
            addBefore(newModifierList, getOrCreateConstructorKeyword())
        }
    }

    override fun removeModifier(modifier: KtModifierKeywordToken) {
        super.removeModifier(modifier)
        if (modifierList == null) {
            removeRedundantConstructorKeywordAndSpace()
        }
    }

    override fun addAnnotationEntry(annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
        val modifierList = modifierList
        return if (modifierList != null) {
            modifierList.addBefore(annotationEntry, modifierList.firstChild) as KtAnnotationEntry
        } else {
            val newModifierList = KtPsiFactory(project).createModifierList(annotationEntry.text)
            (addBefore(newModifierList, getOrCreateConstructorKeyword()) as KtModifierList).annotationEntries.first()
        }
    }

    override fun mayHaveContract(): Boolean = false
}
