/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class Fir2IrBindableSymbol<out D : DeclarationDescriptor, B : IrDeclaration>(
    override val signature: IdSignature,
) : IrBindableSymbol<D, B> {

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("Symbol is unbound")

    override fun bind(owner: B) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("${javaClass.simpleName} for $signature is already bound")
        }
    }

    override val isBound: Boolean
        get() = _owner != null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: D
        @Suppress("UNCHECKED_CAST")
        get() = owner.toIrBasedDescriptor() as D

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = false

    override var privateSignature: IdSignature? = null

    override fun toString(): String {
        if (isBound) return owner.render()
        return "Unbound public symbol for $signature"
    }
}
