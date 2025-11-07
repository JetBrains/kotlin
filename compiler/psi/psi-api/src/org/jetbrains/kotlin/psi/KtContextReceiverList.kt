/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * The class representing a context receiver list in a declaration or functional type.
 *
 * This class is obsolete and should not be used directly, it is present for compatibility reasons only.
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
