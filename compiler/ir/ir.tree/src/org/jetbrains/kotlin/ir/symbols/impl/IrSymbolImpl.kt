/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.isPublicApi
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

abstract class IrSymbolBase<Owner : IrSymbolOwner>(
) : IrSymbol {
    private var _owner: Owner? = null
    final override val owner: Owner
        get() = _owner // Please keep `_owner` and `?: error()` at separate lines to make it easier to set different breakpoints.
            ?: error("${javaClass.simpleName} is unbound. Signature: $signature")

    override val signature: IdSignature?
        get() = null

    final override var privateSignature: IdSignature? = null

    final override val isBound: Boolean
        get() = _owner != null

    fun bind(owner: Owner) {
        if (_owner == null) {
            _owner = owner
        } else {
            error("${javaClass.simpleName} is already bound. Signature: $signature. Owner: ${_owner?.render()}")
        }
    }

    override fun toString(): String {
        if (isBound) return owner.render()
        return if (isPublicApi)
            "Unbound public symbol ${this::class.java.simpleName}: $signature"
        else
            "Unbound private symbol ${this::class.java.simpleName}: ${super.toString()}"
    }
}

abstract class IrSymbolWithSignature<Owner : IrSymbolOwner>(
    override val signature: IdSignature?,
) : IrSymbolBase<Owner>()
