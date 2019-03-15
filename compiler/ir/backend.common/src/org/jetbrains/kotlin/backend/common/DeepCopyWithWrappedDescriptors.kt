/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/*
    TODO: Does not preserve getter/setter descriptors as implementations of PropertyAccessorDescriptor.
    This causes problems for KotlinTypeMapper.mapFunctionName (maybe other places as well)
 */

inline fun <reified T : IrElement> T.deepCopyWithWrappedDescriptors(initialParent: IrDeclarationParent? = null): T =
    deepCopyWithSymbols(initialParent, DescriptorsToIrRemapper).also {
        it.acceptVoid(WrappedDescriptorPatcher)
    }

object DescriptorsToIrRemapper : DescriptorsRemapper {
    override fun remapDeclaredClass(descriptor: ClassDescriptor) =
        WrappedClassDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredConstructor(descriptor: ClassConstructorDescriptor) =
        WrappedClassConstructorDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredEnumEntry(descriptor: ClassDescriptor) =
        WrappedClassDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredField(descriptor: PropertyDescriptor) =
        WrappedFieldDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor) =
        WrappedSimpleFunctionDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredTypeParameter(descriptor: TypeParameterDescriptor) =
        WrappedTypeParameterDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredVariable(descriptor: VariableDescriptor) =
        WrappedVariableDescriptor(descriptor.annotations, descriptor.source)

    override fun remapDeclaredValueParameter(descriptor: ParameterDescriptor): ParameterDescriptor =
        if (descriptor is ReceiverParameterDescriptor)
            WrappedReceiverParameterDescriptor(descriptor.annotations, descriptor.source)
        else
            WrappedValueParameterDescriptor(descriptor.annotations, descriptor.source)
}

object WrappedDescriptorPatcher : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        (declaration.descriptor as WrappedClassDescriptor).bind(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        (declaration.descriptor as WrappedClassConstructorDescriptor).bind(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        (declaration.descriptor as WrappedClassDescriptor).bind(
            declaration.correspondingClass ?: declaration.parentAsClass
        )
        declaration.acceptChildrenVoid(this)
    }

    override fun visitField(declaration: IrField) {
        (declaration.descriptor as WrappedFieldDescriptor).bind(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        (declaration.descriptor as WrappedSimpleFunctionDescriptor).bind(declaration as IrSimpleFunction)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        (declaration.descriptor as? WrappedValueParameterDescriptor)?.bind(declaration)
        (declaration.descriptor as? WrappedReceiverParameterDescriptor)?.bind(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        (declaration.descriptor as WrappedTypeParameterDescriptor).bind(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        (declaration.descriptor as WrappedVariableDescriptor).bind(declaration)
        declaration.acceptChildrenVoid(this)
    }
}
