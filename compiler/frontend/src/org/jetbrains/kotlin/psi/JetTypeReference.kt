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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.psiUtil.collectAnnotationEntriesFromStubOrPsi
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

/**
 * Type reference element.
 * Underlying token is [org.jetbrains.kotlin.JetNodeTypes.TYPE_REFERENCE]
 */
class JetTypeReference : JetElementImplStub<KotlinPlaceHolderStub<JetTypeReference>>, JetAnnotated, JetAnnotationsContainer {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<JetTypeReference>) : super(stub, JetStubElementTypes.TYPE_REFERENCE)

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    val typeElement: JetTypeElement?
        get() = JetStubbedPsiUtil.getStubOrPsiChild(this, JetStubElementTypes.TYPE_ELEMENT_TYPES, JetTypeElement.ARRAY_FACTORY)

    override fun getAnnotations(): List<JetAnnotation> {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.ANNOTATION)
    }

    override fun getAnnotationEntries(): List<JetAnnotationEntry> {
        return this.collectAnnotationEntriesFromStubOrPsi()
    }

    fun hasParentheses(): Boolean {
        return findChildByType<PsiElement>(JetTokens.LPAR) != null && findChildByType<PsiElement>(JetTokens.LPAR) != null
    }
}
