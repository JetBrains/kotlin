/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 * @sample org.jetbrains.kotlin.ir.generator.IrTree.functionReference
 */
abstract class IrFunctionReference : IrCallableReference<IrFunctionSymbol>() {
    abstract val reflectionTarget: IrFunctionSymbol?

    abstract var hasExtensionReceiver: Boolean
    abstract var contextReceiversCount: Int

    override val receiversPrefixSize: Int
        get() = contextReceiversCount + (if (hasExtensionReceiver) 1 else 0)

    override var extensionReceiver: IrExpression?
        get() = if (hasExtensionReceiver) getValueArgument(contextReceiversCount) else null
        set(value) {
            if (value == null) return
            require(hasExtensionReceiver) { "Trying to set extension receiver for non-extension" }

            putValueArgument(contextReceiversCount, value)
        }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitFunctionReference(this, data)
}
