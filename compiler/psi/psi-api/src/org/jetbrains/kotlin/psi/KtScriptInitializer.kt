/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.utils.sure

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
@OptIn(KtImplementationDetail::class)
class KtScriptInitializer : KtDeclarationStub<KotlinPlaceHolderStub<KtScriptInitializer>>, KtAnonymousInitializer {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinPlaceHolderStub<KtScriptInitializer>) : super(stub, KtStubBasedElementTypes.SCRIPT_INITIALIZER)

    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    /** The [KtScript] that contains this top-level script initializer. */
    override val containingDeclaration: KtScript
        get() = getParentOfType<KtScript>(true).sure { "Should only be present in script" }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitScriptInitializer(this, data)
}
