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
import org.jetbrains.kotlin.backend.konan.lower.SuspendFunctionsLowering
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

abstract class DescriptorWithIrBuilder<out D: DeclarationDescriptor, out B: IrDeclaration> {

    protected abstract fun buildDescriptor(): D

    protected open fun doInitialize() { }

    protected abstract fun buildIr(): B

    val descriptor by lazy { buildDescriptor() }

    private val builtIr by lazy { buildIr() }
    private var initialized: Boolean = false

    fun initialize() {
        doInitialize()
        initialized = true
    }

    val ir: B
        get() {
            if (!initialized)
                throw Error("Access to IR before initialization")
            return builtIr
        }
}

fun BackendContext.createPropertyGetterBuilder(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
                                               propertyDescriptor: PropertyDescriptor, type: KotlinType)
        = object: DescriptorWithIrBuilder<PropertyGetterDescriptorImpl, IrFunction>() {

    override fun buildDescriptor() = PropertyGetterDescriptorImpl(
            /* correspondingProperty = */ propertyDescriptor,
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

    override fun doInitialize() {
        descriptor.apply {
            initialize(type)
        }
    }

    override fun buildIr() = IrFunctionImpl(
            startOffset = startOffset,
            endOffset   = endOffset,
            origin      = origin,
            descriptor  = descriptor).apply {

        createParameterDeclarations()

        body = createIrBuilder(descriptor, startOffset, endOffset).irBlockBody {
            +irReturn(irGetField(irThis(), propertyDescriptor))
        }
    }
}

private fun BackendContext.createPropertySetterBuilder(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
                                                       propertyDescriptor: PropertyDescriptor, type: KotlinType)
        = object: DescriptorWithIrBuilder<PropertySetterDescriptorImpl, IrFunction>() {

    override fun buildDescriptor() = PropertySetterDescriptorImpl(
            /* correspondingProperty = */ propertyDescriptor,
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

    lateinit var valueParameterDescriptor: ValueParameterDescriptor

    override fun doInitialize() {
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
            descriptor  = descriptor).apply {

        createParameterDeclarations()

        body = createIrBuilder(descriptor, startOffset, endOffset).irBlockBody {
            +irSetField(irThis(), propertyDescriptor, irGet(valueParameterDescriptor))
        }
    }
}

fun BackendContext.createPropertyWithBackingFieldBuilder(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
                                                         owner: ClassDescriptor, name: Name, type: KotlinType, isMutable: Boolean)
        = object: DescriptorWithIrBuilder<PropertyDescriptorImpl, IrProperty>() {

    private lateinit var getterBuilder: DescriptorWithIrBuilder<PropertyGetterDescriptorImpl, IrFunction>
    private var setterBuilder: DescriptorWithIrBuilder<PropertySetterDescriptorImpl, IrFunction>? = null

    override fun buildDescriptor() = PropertyDescriptorImpl.create(
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
            /* isDelegated           = */ false)

    override fun doInitialize() {
        getterBuilder = createPropertyGetterBuilder(startOffset, endOffset, origin, descriptor, type).apply { initialize() }
        if (isMutable)
            setterBuilder = createPropertySetterBuilder(startOffset, endOffset, origin, descriptor, type).apply { initialize() }
        descriptor.initialize(getterBuilder.descriptor, setterBuilder?.descriptor)
        val receiverType: KotlinType? = null
        descriptor.setType(type, emptyList(), owner.thisAsReceiverParameter, receiverType)
    }

    override fun buildIr(): IrProperty {
        val backingField = IrFieldImpl(
                startOffset = startOffset,
                endOffset   = endOffset,
                origin      = origin,
                descriptor  = descriptor)
        return IrPropertyImpl(
                startOffset  = startOffset,
                endOffset    = endOffset,
                origin       = origin,
                isDelegated  = false,
                descriptor   = descriptor,
                backingField = backingField,
                getter       = getterBuilder.ir,
                setter       = setterBuilder?.ir)
    }
}
