/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

open class KtClass : KtClassOrObject {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassStub) : super(stub, KtStubElementTypes.CLASS)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitClass(this, data)
    }

    private val _stub: KotlinClassStub?
        get() = stub as? KotlinClassStub

    fun getProperties(): List<KtProperty> = body?.properties.orEmpty()

    fun isInterface(): Boolean =
        _stub?.isInterface() ?: (findChildByType<PsiElement>(KtTokens.INTERFACE_KEYWORD) != null)

    fun isEnum(): Boolean = hasModifier(KtTokens.ENUM_KEYWORD)
    fun isData(): Boolean = hasModifier(KtTokens.DATA_KEYWORD)
    fun isSealed(): Boolean = hasModifier(KtTokens.SEALED_KEYWORD)
    fun isInner(): Boolean = hasModifier(KtTokens.INNER_KEYWORD)
    fun isInline(): Boolean = hasModifier(KtTokens.INLINE_KEYWORD)
    fun isValue(): Boolean = hasModifier(KtTokens.VALUE_KEYWORD)

    override fun getCompanionObjects(): List<KtObjectDeclaration> = body?.allCompanionObjects.orEmpty()

    fun getClassOrInterfaceKeyword(): PsiElement? = findChildByType(TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD))

    fun getClassKeyword(): PsiElement? = findChildByType(KtTokens.CLASS_KEYWORD)

    fun getFunKeyword(): PsiElement? = modifierList?.getModifier(KtTokens.FUN_KEYWORD)
}

fun KtClass.createPrimaryConstructorIfAbsent(): KtPrimaryConstructor {
    val constructor = primaryConstructor
    if (constructor != null) return constructor
    var anchor: PsiElement? = typeParameterList
    if (anchor == null) anchor = nameIdentifier
    if (anchor == null) anchor = lastChild
    return addAfter(KtPsiFactory(project).createPrimaryConstructor(), anchor) as KtPrimaryConstructor
}

fun KtClass.createPrimaryConstructorParameterListIfAbsent(): KtParameterList {
    val constructor = createPrimaryConstructorIfAbsent()
    val parameterList = constructor.valueParameterList
    if (parameterList != null) return parameterList
    return constructor.add(KtPsiFactory(project).createParameterList("()")) as KtParameterList
}