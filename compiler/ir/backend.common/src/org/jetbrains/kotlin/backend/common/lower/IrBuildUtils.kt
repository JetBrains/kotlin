/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

abstract class SymbolWithIrBuilder<out S: IrSymbol, out D: IrDeclaration> {

    protected abstract fun buildSymbol(): S

    protected open fun doInitialize() { }

    protected abstract fun buildIr(): D

    val symbol by lazy { buildSymbol() }

    private val builtIr by lazy { buildIr() }
    private var initialized: Boolean = false

    fun initialize() {
        doInitialize()
        initialized = true
    }

    val ir: D
        get() {
            if (!initialized)
                throw Error("Access to IR before initialization")
            return builtIr
        }
}

fun BackendContext.createPropertyGetterBuilder(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
                                               fieldSymbol: IrFieldSymbol, type: KotlinType)
        = object: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

    override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
            PropertyGetterDescriptorImpl(
                    /* correspondingProperty = */ fieldSymbol.descriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isDefault             = */ false,
                    /* isExternal            = */ false,
                    /* isInline              = */ false,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* original              = */ null,
                    /* source                = */ SourceElement.NO_SOURCE
            )
    )

    override fun doInitialize() {
        val descriptor = symbol.descriptor as PropertyGetterDescriptorImpl
        descriptor.apply {
            initialize(type)
        }
    }

    override fun buildIr() = IrFunctionImpl(
            startOffset = startOffset,
            endOffset   = endOffset,
            origin      = origin,
            symbol      = symbol).apply {

        createParameterDeclarations()

        body = createIrBuilder(this.symbol, startOffset, endOffset).irBlockBody {
            +irReturn(irGetField(irGet(this@apply.dispatchReceiverParameter!!.symbol), fieldSymbol))
        }
    }
}

private fun BackendContext.createPropertySetterBuilder(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
                                                       fieldSymbol: IrFieldSymbol, type: KotlinType)
        = object: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

    override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
            PropertySetterDescriptorImpl(
                    /* correspondingProperty = */ fieldSymbol.descriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isDefault             = */ false,
                    /* isExternal            = */ false,
                    /* isInline              = */ false,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* original              = */ null,
                    /* source                = */ SourceElement.NO_SOURCE
            )
    )

    lateinit var valueParameterDescriptor: ValueParameterDescriptor

    override fun doInitialize() {
        val descriptor = symbol.descriptor as PropertySetterDescriptorImpl
        descriptor.apply {
            valueParameterDescriptor = ValueParameterDescriptorImpl(
                    containingDeclaration = this,
                    original              = null,
                    index                 = 0,
                    annotations           = Annotations.EMPTY,
                    name                  = Name.identifier("value"),
                    outType               = type,
                    declaresDefaultValue  = false,
                    isCrossinline         = false,
                    isNoinline            = false,
                    varargElementType     = null,
                    source                = SourceElement.NO_SOURCE
            )

            initialize(valueParameterDescriptor)
        }
    }

    override fun buildIr() = IrFunctionImpl(
            startOffset = startOffset,
            endOffset   = endOffset,
            origin      = origin,
            symbol      = symbol).apply {

        createParameterDeclarations()

        body = createIrBuilder(this.symbol, startOffset, endOffset).irBlockBody {
            +irSetField(irGet(this@apply.dispatchReceiverParameter!!.symbol), fieldSymbol, irGet(this@apply.valueParameters.single().symbol))
        }
    }
}

fun BackendContext.createPropertyWithBackingFieldBuilder(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
                                                         owner: ClassDescriptor, name: Name, type: KotlinType, isMutable: Boolean)
        = object: SymbolWithIrBuilder<IrFieldSymbol, IrProperty>() {

    private lateinit var getterBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>
    private var setterBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null

    override fun buildSymbol() = IrFieldSymbolImpl(
            PropertyDescriptorImpl.create(
                    /* containingDeclaration = */ owner,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isVar                 = */ isMutable,
                    /* name                  = */ name,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* lateInit              = */ false,
                    /* isConst               = */ false,
                    /* isHeader              = */ false,
                    /* isImpl                = */ false,
                    /* isExternal            = */ false,
                    /* isDelegated           = */ false
            )
    )

    override fun doInitialize() {
        val descriptor = symbol.descriptor as PropertyDescriptorImpl
        getterBuilder = createPropertyGetterBuilder(startOffset, endOffset, origin, symbol, type).apply { initialize() }
        if (isMutable)
            setterBuilder = createPropertySetterBuilder(startOffset, endOffset, origin, symbol, type).apply { initialize() }
        descriptor.initialize(
                /* getter = */ getterBuilder.symbol.descriptor as PropertyGetterDescriptorImpl,
                /* setter = */ setterBuilder?.symbol?.descriptor as? PropertySetterDescriptorImpl)
        val receiverType: KotlinType? = null
        descriptor.setType(type, emptyList(), owner.thisAsReceiverParameter, receiverType)
    }

    override fun buildIr(): IrProperty {
        val backingField = IrFieldImpl(
                startOffset = startOffset,
                endOffset   = endOffset,
                origin      = origin,
                symbol      = symbol)
        return IrPropertyImpl(
                startOffset  = startOffset,
                endOffset    = endOffset,
                origin       = origin,
                isDelegated  = false,
                descriptor   = symbol.descriptor,
                backingField = backingField,
                getter       = getterBuilder.ir,
                setter       = setterBuilder?.ir)
    }
}
