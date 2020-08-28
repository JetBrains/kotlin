/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class Fir2IrBindableSymbol<out D : DeclarationDescriptor, B : IrSymbolOwner>(
    override val signature: IdSignature,
    private val containerSource: DeserializedContainerSource? = null
) : IrBindableSymbol<D, B> {

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("Symbol is unbound")

    override fun bind(owner: B) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("${javaClass.simpleName} is already bound")
        }
    }

    override val isPublicApi: Boolean = true

    override val isBound: Boolean
        get() = _owner != null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: D by lazy {
        val result = when (val owner = owner) {
            is IrEnumEntry -> WrappedEnumEntryDescriptor().apply { bind(owner) }
            is IrClass -> WrappedClassDescriptor().apply { bind(owner) }
            is IrConstructor -> WrappedClassConstructorDescriptor().apply { bind(owner) }
            is IrSimpleFunction -> when {
                containerSource != null ->
                    WrappedFunctionDescriptorWithContainerSource()
                owner.name.isSpecial && owner.name.asString().startsWith(GETTER_PREFIX) ->
                    WrappedPropertyGetterDescriptor(Annotations.EMPTY, SourceElement.NO_SOURCE)
                owner.name.isSpecial && owner.name.asString().startsWith(SETTER_PREFIX) ->
                    WrappedPropertySetterDescriptor(Annotations.EMPTY, SourceElement.NO_SOURCE)
                else ->
                    WrappedSimpleFunctionDescriptor()
            }.apply { bind(owner) }
            is IrVariable -> WrappedVariableDescriptor().apply { bind(owner) }
            is IrValueParameter -> WrappedValueParameterDescriptor().apply { bind(owner) }
            is IrTypeParameter -> WrappedTypeParameterDescriptor().apply { bind(owner) }
            is IrProperty -> if (containerSource != null) {
                WrappedPropertyDescriptorWithContainerSource()
            } else {
                WrappedPropertyDescriptor()
            }.apply { bind(owner) }
            is IrField -> WrappedFieldDescriptor().apply { bind(owner) }
            is IrTypeAlias -> WrappedTypeAliasDescriptor().apply { bind(owner) }
            else -> throw IllegalStateException("Unsupported owner in Fir2IrBindableSymbol: $owner")
        }

        @Suppress("UNCHECKED_CAST")
        result as D
    }

    companion object {
        private const val GETTER_PREFIX = "<get"
        private const val SETTER_PREFIX = "<set"
    }
}
