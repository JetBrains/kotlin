/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.collectAnnotationEntriesFromStubOrPsi
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * Type reference element.
 * Underlying token is [org.jetbrains.kotlin.KtNodeTypes.TYPE_REFERENCE]
 */
class KtTypeReference : KtModifierListOwnerStub<KotlinPlaceHolderStub<KtTypeReference>>,
    KtAnnotated, KtAnnotationsContainer {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtTypeReference>) : super(stub, KtStubElementTypes.TYPE_REFERENCE)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    val typeElement: KtTypeElement?
        get() = KtStubbedPsiUtil.getStubOrPsiChild(this, KtStubElementTypes.TYPE_ELEMENT_TYPES, KtTypeElement.ARRAY_FACTORY)

    override fun getAnnotations(): List<KtAnnotation> {
        return modifierList?.annotations.orEmpty()
    }

    override fun getAnnotationEntries(): List<KtAnnotationEntry> {
        return modifierList?.annotationEntries.orEmpty()
    }

    fun hasParentheses(): Boolean {
        return findChildByType<PsiElement>(KtTokens.LPAR) != null && findChildByType<PsiElement>(KtTokens.RPAR) != null
    }

    fun nameForReceiverLabel() = if (typeElement is KtUserType) (typeElement as KtUserType).referencedName else null
}
