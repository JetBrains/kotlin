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
@OptIn(KtImplementationDetail::class)
class KtClassInitializer : KtDeclarationStub<KotlinPlaceHolderStub<KtClassInitializer>>, KtAnonymousInitializer {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinPlaceHolderStub<KtClassInitializer>) : super(stub, KtStubBasedElementTypes.CLASS_INITIALIZER)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitClassInitializer(this, data)

    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    /**
     * The opening brace of the `init` block's body, or `null` if the body is not a block.
     */
    val openBraceNode: PsiElement?
        get() = (body as? KtBlockExpression)?.lBrace

    /**
     * The `init` keyword introducing this block.
     */
    val initKeyword: PsiElement
        get() = findChildByType(KtTokens.INIT_KEYWORD)!!

    /** The [KtClassOrObject] that declares this `init` block. */
    override val containingDeclaration: KtClassOrObject
        get() = getParentOfType<KtClassOrObject>(true).sure { "Should only be present in class or object" }
}
