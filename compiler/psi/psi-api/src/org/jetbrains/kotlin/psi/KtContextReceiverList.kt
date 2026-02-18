/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * Represents a list of context receivers declared on a function, property, or class.
 *
 * ### Example:
 *
 * ```kotlin
 *    context(Logger, Config)
 * // ^_____________________^
 * fun process() {}
 * ```
 *
 * This class is obsolete and should not be used directly; it is present for compatibility reasons only.
 *
 * Use [KtContextParameterList] instead.
 */
@OptIn(KtImplementationDetail::class)
class KtContextReceiverList : KtContextParameterList {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtContextParameterList>) : super(stub)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        // This is a hack to make the context parameter list overrides and context receiver list overrides work for this class
        return visitor.visitContextParameterList(this, data) ?: visitor.visitContextReceiverList(this, data)
    }
}
