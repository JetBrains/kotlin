/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.utils.sure

interface KtAnonymousInitializer : KtDeclaration, KtStatementExpression {
    val containingDeclaration: KtDeclaration
    val body: KtExpression?
}

/**
 * Represents an init block in a class that executes during instance initialization.
 *
 * ### Example:
 *
 * ```kotlin
 * class Foo {
 *     init {
 *         println("initialized")
 *     }
 * //  ^________________________^
 * //  The entire 'init' block
 * }
 * ```
 */
class KtClassInitializer : KtDeclarationStub<KotlinPlaceHolderStub<KtClassInitializer>>, KtAnonymousInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtClassInitializer>) : super(stub, KtStubBasedElementTypes.CLASS_INITIALIZER)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitClassInitializer(this, data)

    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    val openBraceNode: PsiElement?
        get() = (body as? KtBlockExpression)?.lBrace

    val initKeyword: PsiElement
        get() = findChildByType(KtTokens.INIT_KEYWORD)!!

    override val containingDeclaration: KtClassOrObject
        get() = getParentOfType<KtClassOrObject>(true).sure { "Should only be present in class or object" }
}

/**
 * Represents an initializer expression in a script. Effectively, it is a wrapper for top-level expressions
 *
 * ### Example:
 *
 * ```kotlin
 * // In a .kts file:
 *    println("Hello")
 * // ^______________^
 * ```
 */
class KtScriptInitializer : KtDeclarationStub<KotlinPlaceHolderStub<KtScriptInitializer>>, KtAnonymousInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtScriptInitializer>) : super(stub, KtStubBasedElementTypes.SCRIPT_INITIALIZER)

    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override val containingDeclaration: KtScript
        get() = getParentOfType<KtScript>(true).sure { "Should only be present in script" }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitScriptInitializer(this, data)
}