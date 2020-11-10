/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.ReturnTypeIsNotInitializedException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrFunctionCommonImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val name: Name,
    override var visibility: DescriptorVisibility,
    returnType: IrType,
    protected var flags: Int,
    override val containerSource: DeserializedContainerSource?,
) : IrSimpleFunction() {
    override val factory: IrFactory
        get() = IrFactoryImpl

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var returnType: IrType = returnType
        get() = if (field === IrUninitializedType) {
            throw ReturnTypeIsNotInitializedException(this)
        } else {
            field
        }

    override var modality: Modality
        get() = flags.toModality()
        set(value) {
            flags = flags.setModality(value)
        }

    override val isInline
        get() = flags.getFlag(IrFlags.IS_INLINE)

    override val isExternal
        get() = flags.getFlag(IrFlags.IS_EXTERNAL)

    override val isTailrec
        get() = flags.getFlag(IrFlags.IS_TAILREC)

    override val isSuspend
        get() = flags.getFlag(IrFlags.IS_SUSPEND)

    override val isOperator
        get() = flags.getFlag(IrFlags.IS_OPERATOR)

    override val isInfix
        get() = flags.getFlag(IrFlags.IS_INFIX)

    override val isExpect
        get() = flags.getFlag(IrFlags.IS_EXPECT)

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var dispatchReceiverParameter: IrValueParameter? = null
    override var extensionReceiverParameter: IrValueParameter? = null
    override var valueParameters: List<IrValueParameter> = emptyList()

    override var body: IrBody? = null

    override var metadata: MetadataSource? = null

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> = emptyList()

    @Suppress("LeakingThis")
    override var attributeOwnerId: IrAttributeContainer = this

    override var correspondingPropertySymbol: IrPropertySymbol? = null
}

class IrFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    name: Name,
    visibility: DescriptorVisibility,
    modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    isExpect: Boolean,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource? = null,
) : IrFunctionCommonImpl(
    startOffset, endOffset, origin, name, visibility, returnType,
    modality.toFlags() or isInline.toFlag(IrFlags.IS_INLINE) or isExternal.toFlag(IrFlags.IS_EXTERNAL) or
            isTailrec.toFlag(IrFlags.IS_TAILREC) or isSuspend.toFlag(IrFlags.IS_SUSPEND) or isOperator.toFlag(IrFlags.IS_OPERATOR) or
            isInfix.toFlag(IrFlags.IS_INFIX) or isExpect.toFlag(IrFlags.IS_EXPECT),
    containerSource,
) {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }
}

class IrFakeOverrideFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    override var visibility: DescriptorVisibility,
    modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    isExpect: Boolean,
) : IrFunctionCommonImpl(
    startOffset, endOffset, origin, name, visibility, returnType,
    modality.toFlags() or isInline.toFlag(IrFlags.IS_INLINE) or isExternal.toFlag(IrFlags.IS_EXTERNAL) or
            isTailrec.toFlag(IrFlags.IS_TAILREC) or isSuspend.toFlag(IrFlags.IS_SUSPEND) or isOperator.toFlag(IrFlags.IS_OPERATOR) or
            isInfix.toFlag(IrFlags.IS_INFIX) or isExpect.toFlag(IrFlags.IS_EXPECT),
    containerSource = null,
), IrFakeOverrideFunction {
    override val isFakeOverride: Boolean
        get() = true

    private var _symbol: IrSimpleFunctionSymbol? = null

    override val symbol: IrSimpleFunctionSymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @ObsoleteDescriptorBasedAPI
    override val descriptor
        get() = _symbol?.descriptor ?: WrappedSimpleFunctionDescriptor()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun acquireSymbol(symbol: IrSimpleFunctionSymbol): IrSimpleFunction {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        (symbol.descriptor as? WrappedSimpleFunctionDescriptor)?.bind(this)
        return this
    }
}
