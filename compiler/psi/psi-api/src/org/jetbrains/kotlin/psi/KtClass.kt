/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub

/**
 * Represents a class or interface declaration.
 *
 * ### Example:
 *
 * ```kotlin
 *    class Foo(val x: Int) {
 *        fun bar() {}
 *    }
 * // ^________________^
 * // The entire class
 * ```
 */
open class KtClass : KtClassOrObject {
    private val classInterfaceTokenSet = TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD)

    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassStub) : super(stub, KtStubBasedElementTypes.CLASS)
    constructor(stub: KotlinClassStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitClass(this, data)
    }

    private val _stub: KotlinClassStub?
        get() = greenStub as? KotlinClassStub

    fun getProperties(): List<KtProperty> = body?.properties.orEmpty()

    fun isInterface(): Boolean =
        _stub?.isInterface ?: (findChildByType<PsiElement>(KtTokens.INTERFACE_KEYWORD) != null)

    fun isEnum(): Boolean = hasModifier(KtTokens.ENUM_KEYWORD)
    fun isSealed(): Boolean = hasModifier(KtTokens.SEALED_KEYWORD)
    fun isInner(): Boolean = hasModifier(KtTokens.INNER_KEYWORD)
    fun isInline(): Boolean = hasModifier(KtTokens.INLINE_KEYWORD)
    fun isValue(): Boolean = hasModifier(KtTokens.VALUE_KEYWORD)

    override fun getCompanionObjects(): List<KtObjectDeclaration> = body?.allCompanionObjects.orEmpty()

    fun getClassOrInterfaceKeyword(): PsiElement? = findChildByType(classInterfaceTokenSet)

    fun getClassKeyword(): PsiElement? = findChildByType(KtTokens.CLASS_KEYWORD)

    fun getFunKeyword(): PsiElement? = modifierList?.getModifier(KtTokens.FUN_KEYWORD)
}

@Deprecated(
    "Use getOrCreatePrimaryConstructor() instead",
    ReplaceWith(
        "this.getOrCreatePrimaryConstructor()",
        "org.jetbrains.kotlin.idea.base.psi.getOrCreatePrimaryConstructor",
    ),
)
@OptIn(KtNonPublicApi::class)
fun KtClass.createPrimaryConstructorIfAbsent(): KtPrimaryConstructor =
    KtPsiMutationService.getInstance().getOrCreatePrimaryConstructor(this)

@Deprecated(
    "Use getOrCreatePrimaryConstructorParameterList() instead",
    ReplaceWith(
        "this.getOrCreatePrimaryConstructorParameterList()",
        "org.jetbrains.kotlin.idea.base.psi.getOrCreatePrimaryConstructorParameterList",
    ),
)
@OptIn(KtNonPublicApi::class)
fun KtClass.createPrimaryConstructorParameterListIfAbsent(): KtParameterList =
    KtPsiMutationService.getInstance().getOrCreatePrimaryConstructorParameterList(this)
