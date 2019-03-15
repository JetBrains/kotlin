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
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.*
import java.util.*

class KtClassBody : KtElementImplStub<KotlinPlaceHolderStub<KtClassBody>>, KtDeclarationContainer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtClassBody>) : super(stub, CLASS_BODY)

    override fun getParent() = parentByStub

    override fun getDeclarations() = Arrays.asList(*getStubOrPsiChildren(DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY))

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitClassBody(this, data)

    val anonymousInitializers: List<KtAnonymousInitializer>
        get() = findChildrenByType(KtNodeTypes.CLASS_INITIALIZER)

    internal val secondaryConstructors: List<KtSecondaryConstructor>
        get() = getStubOrPsiChildrenAsList(KtStubElementTypes.SECONDARY_CONSTRUCTOR)

    val properties: List<KtProperty>
        get() = getStubOrPsiChildrenAsList(KtStubElementTypes.PROPERTY)

    val enumEntries: List<KtEnumEntry>
        get() = getStubOrPsiChildrenAsList(KtStubElementTypes.ENUM_ENTRY).filterIsInstance<KtEnumEntry>()

    val allCompanionObjects: List<KtObjectDeclaration>
        get() = getStubOrPsiChildrenAsList(KtStubElementTypes.OBJECT_DECLARATION).filter { it.isCompanion() }

    val rBrace: PsiElement?
        get() = node.getChildren(TokenSet.create(KtTokens.RBRACE)).singleOrNull()?.psi

    val lBrace: PsiElement?
        get() = node.getChildren(TokenSet.create(KtTokens.LBRACE)).singleOrNull()?.psi

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    val danglingAnnotations: List<KtAnnotationEntry>
        get() = getStubOrPsiChildrenAsList(MODIFIER_LIST).flatMap { it.annotationEntries }
}
