/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtPrimaryConstructor : KtConstructor<KtPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtPrimaryConstructor>) : super(stub, KtStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingClassOrObject() = parent as KtClassOrObject

    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getConstructorKeyword() ?: addBefore(KtPsiFactory(this).createConstructorKeyword(), valueParameterList!!)
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
            val newModifierList = KtPsiFactory(this).createModifierList(modifier)
            addBefore(newModifierList, getOrCreateConstructorKeyword())
        }
    }

    override fun removeModifier(modifier: KtModifierKeywordToken) {
        super.removeModifier(modifier)
        if (modifierList == null) {
            getConstructorKeyword()?.delete()
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
}
