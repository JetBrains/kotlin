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
    protected var flags: Short,
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
        get() = flags.toInt().toModality()
        set(value) {
            flags = flags.toInt().setModality(value).toShort()
        }

    override val isInline
        get() = flags.toInt().getFlag(IS_INLINE_BIT)

    override val isExternal
        get() = flags.toInt().getFlag(IS_EXTERNAL_BIT)

    override val isTailrec
        get() = flags.toInt().getFlag(IS_TAILREC_BIT)

    override val isSuspend
        get() = flags.toInt().getFlag(IS_SUSPEND_BIT)

    override val isOperator
        get() = flags.toInt().getFlag(IS_OPERATOR_BIT)

    override val isInfix
        get() = flags.toInt().getFlag(IS_INFIX_BIT)

    override val isExpect
        get() = flags.toInt().getFlag(IS_EXPECT_BIT)

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

    protected companion object {
        const val IS_INLINE_BIT = 1 shl (IrFlags.MODALITY_BITS + 0)
        const val IS_EXTERNAL_BIT = 1 shl (IrFlags.MODALITY_BITS + 1)
        const val IS_TAILREC_BIT = 1 shl (IrFlags.MODALITY_BITS + 2)
        const val IS_SUSPEND_BIT = 1 shl (IrFlags.MODALITY_BITS + 3)
        const val IS_OPERATOR_BIT = 1 shl (IrFlags.MODALITY_BITS + 4)
        const val IS_INFIX_BIT = 1 shl (IrFlags.MODALITY_BITS + 5)
        const val IS_EXPECT_BIT = 1 shl (IrFlags.MODALITY_BITS + 6)
        const val IS_FAKE_OVERRIDE_BIT = 1 shl (IrFlags.MODALITY_BITS + 7)
    }
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
    isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource? = null,
) : IrFunctionCommonImpl(
    startOffset, endOffset, origin, name, visibility, returnType,
    (modality.toFlags() or isInline.toFlag(IS_INLINE_BIT) or isExternal.toFlag(IS_EXTERNAL_BIT) or
            isTailrec.toFlag(IS_TAILREC_BIT) or isSuspend.toFlag(IS_SUSPEND_BIT) or isOperator.toFlag(IS_OPERATOR_BIT) or
            isInfix.toFlag(IS_INFIX_BIT) or isExpect.toFlag(IS_EXPECT_BIT) or isFakeOverride.toFlag(IS_FAKE_OVERRIDE_BIT)).toShort(),
    containerSource,
) {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    override val isFakeOverride: Boolean
        get() = flags.toInt().getFlag(IS_FAKE_OVERRIDE_BIT)

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
    (modality.toFlags() or isInline.toFlag(IS_INLINE_BIT) or isExternal.toFlag(IS_EXTERNAL_BIT) or
            isTailrec.toFlag(IS_TAILREC_BIT) or isSuspend.toFlag(IS_SUSPEND_BIT) or isOperator.toFlag(IS_OPERATOR_BIT) or
            isInfix.toFlag(IS_INFIX_BIT) or isExpect.toFlag(IS_EXPECT_BIT)).toShort(),
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
