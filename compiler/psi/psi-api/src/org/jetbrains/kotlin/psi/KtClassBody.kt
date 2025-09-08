/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

class KtClassBody : KtElementImplStub<KotlinPlaceHolderStub<KtClassBody>>, KtDeclarationContainer {
    private val lBraceTokenSet = TokenSet.create(KtTokens.LBRACE)
    private val rBraceTokenSet = TokenSet.create(KtTokens.RBRACE)

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtClassBody>) : super(stub, KtStubBasedElementTypes.CLASS_BODY)

    override fun getParent() = parentByStub

    override fun getDeclarations() = stub?.getChildrenByType(KtFile.FILE_DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY)?.toList()
        ?: PsiTreeUtil.getChildrenOfTypeAsList(this, KtDeclaration::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitClassBody(this, data)

    val anonymousInitializers: List<KtAnonymousInitializer>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.CLASS_INITIALIZER)

    internal val secondaryConstructors: List<KtSecondaryConstructor>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.SECONDARY_CONSTRUCTOR)

    val properties: List<KtProperty>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.PROPERTY)

    val functions: List<KtNamedFunction>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.FUNCTION)

    val enumEntries: List<KtEnumEntry>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.ENUM_ENTRY).filterIsInstance<KtEnumEntry>()

    val allCompanionObjects: List<KtObjectDeclaration>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.OBJECT_DECLARATION).filter { it.isCompanion() }

    val rBrace: PsiElement?
        get() = node.getChildren(rBraceTokenSet).singleOrNull()?.psi

    val lBrace: PsiElement?
        get() = node.getChildren(lBraceTokenSet).singleOrNull()?.psi

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    val danglingAnnotations: List<KtAnnotationEntry>
        get() = danglingModifierLists.flatMap { it.annotationEntries }

    /**
     * @return modifier lists that do not belong to any declaration due to incomplete code or syntax errors
     */
    val danglingModifierLists: List<KtModifierList>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.MODIFIER_LIST)
}
