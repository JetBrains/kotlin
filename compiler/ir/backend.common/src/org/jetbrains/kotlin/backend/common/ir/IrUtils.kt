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

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.DumpIrTreeWithDescriptorsVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.ir.util.defaultType
import java.io.StringWriter


fun ir2string(ir: IrElement?): String = ir2stringWhole(ir).takeWhile { it != '\n' }

fun ir2stringWhole(ir: IrElement?, withDescriptors: Boolean = false): String {
    val strWriter = StringWriter()

    if (withDescriptors)
        ir?.accept(DumpIrTreeWithDescriptorsVisitor(strWriter), "")
    else
        ir?.accept(DumpIrTreeVisitor(strWriter), "")
    return strWriter.toString()
}

fun DeclarationDescriptor.createFakeOverrideDescriptor(owner: ClassDescriptor): DeclarationDescriptor? {
    // We need to copy descriptors for vtable building, thus take only functions and properties.
    return when (this) {
        is CallableMemberDescriptor ->
            copy(
                /* newOwner      = */ owner,
                /* modality      = */ modality,
                /* visibility    = */ visibility,
                /* kind          = */ CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                /* copyOverrides = */ true
            ).apply {
                overriddenDescriptors += this@createFakeOverrideDescriptor
            }
        else -> null
    }
}

fun FunctionDescriptor.createOverriddenDescriptor(owner: ClassDescriptor, final: Boolean = true): FunctionDescriptor {
    return this.newCopyBuilder()
        .setOwner(owner)
        .setCopyOverrides(true)
        .setModality(if (final) Modality.FINAL else Modality.OPEN)
        .setDispatchReceiverParameter(owner.thisAsReceiverParameter)
        .build()!!.apply {
        overriddenDescriptors += this@createOverriddenDescriptor
    }
}

fun IrClass.addSimpleDelegatingConstructor(
        superConstructor: IrConstructor,
        irBuiltIns: IrBuiltIns,
        origin: IrDeclarationOrigin,
        isPrimary: Boolean = false
): IrConstructor {
    val superConstructorDescriptor = superConstructor.descriptor
    val constructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            /* containingDeclaration = */ this.descriptor,
            /* annotations           = */ Annotations.EMPTY,
            /* isPrimary             = */ isPrimary,
            /* source                = */ SourceElement.NO_SOURCE
    )
    val valueParameters = superConstructor.valueParameters.map {
        val descriptor = it.descriptor as ValueParameterDescriptor
        val newDescriptor = descriptor.copy(constructorDescriptor, descriptor.name, descriptor.index)
        IrValueParameterImpl(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                newDescriptor,
                it.type,
                it.varargElementType
        )
    }


    constructorDescriptor.initialize(
            valueParameters.map { it.descriptor as ValueParameterDescriptor },
            superConstructorDescriptor.visibility
    )
    constructorDescriptor.returnType = superConstructorDescriptor.returnType

    return IrConstructorImpl(startOffset, endOffset, origin, constructorDescriptor).also { constructor ->

        assert(superConstructor.dispatchReceiverParameter == null) // Inner classes aren't supported.

        constructor.valueParameters += valueParameters
        constructor.returnType = this.defaultType

        constructor.body = IrBlockBodyImpl(
                startOffset, endOffset,
                listOf(
                        IrDelegatingConstructorCallImpl(
                                startOffset, endOffset, irBuiltIns.unitType,
                                superConstructor.symbol, superConstructor.descriptor
                        ).apply {
                            constructor.valueParameters.forEachIndexed { idx, parameter ->
                                putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol))
                            }
                        },
                        IrInstanceInitializerCallImpl(startOffset, endOffset, this.symbol, irBuiltIns.unitType)
                )
        )

        constructor.parent = this
        this.declarations.add(constructor)
    }
}
