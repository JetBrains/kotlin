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

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DumpIrTreeWithDescriptorsVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
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

internal fun DeclarationDescriptor.createFakeOverrideDescriptor(owner: ClassDescriptor): DeclarationDescriptor? {
    // We need to copy descriptors for vtable building, thus take only functions and properties.
    return when (this) {
        is CallableMemberDescriptor ->
            copy(
                    /* newOwner      = */ owner,
                    /* modality      = */ modality,
                    /* visibility    = */ visibility,
                    /* kind          = */ CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                    /* copyOverrides = */ true).apply {
                overriddenDescriptors += this@createFakeOverrideDescriptor
            }
        else -> null
    }
}

internal fun FunctionDescriptor.createOverriddenDescriptor(owner: ClassDescriptor, final: Boolean = true): FunctionDescriptor {
    return this.newCopyBuilder()
            .setOwner(owner)
            .setCopyOverrides(true)
            .setModality(if (final) Modality.FINAL else Modality.OPEN)
            .setDispatchReceiverParameter(owner.thisAsReceiverParameter)
            .build()!!.apply {
        overriddenDescriptors += this@createOverriddenDescriptor
    }
}

internal fun ClassDescriptor.createSimpleDelegatingConstructorDescriptor(superConstructorDescriptor: ClassConstructorDescriptor, isPrimary: Boolean = false)
        : ClassConstructorDescriptor {
    val constructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            /* containingDeclaration = */ this,
            /* annotations           = */ Annotations.EMPTY,
            /* isPrimary             = */ isPrimary,
            /* source                = */ SourceElement.NO_SOURCE)
    val valueParameters = superConstructorDescriptor.valueParameters.map {
        it.copy(constructorDescriptor, it.name, it.index)
    }
    constructorDescriptor.initialize(valueParameters, superConstructorDescriptor.visibility)
    constructorDescriptor.returnType = superConstructorDescriptor.returnType
    return constructorDescriptor
}

internal fun IrClass.addSimpleDelegatingConstructor(superConstructorSymbol: IrConstructorSymbol,
                                                    constructorDescriptor: ClassConstructorDescriptor,
                                                    origin: IrDeclarationOrigin)
        : IrConstructor {

    return IrConstructorImpl(startOffset, endOffset, origin, constructorDescriptor).also { constructor ->
        constructor.createParameterDeclarations()

        constructor.body = IrBlockBodyImpl(startOffset, endOffset,
                listOf(
                        IrDelegatingConstructorCallImpl(
                                startOffset, endOffset,
                                superConstructorSymbol, superConstructorSymbol.descriptor
                        ).apply {
                            constructor.valueParameters.forEachIndexed { idx, parameter ->
                                putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter.symbol))
                            }
                        },
                        IrInstanceInitializerCallImpl(startOffset, endOffset, this.symbol)
                )
        )

        this.declarations.add(constructor)
    }
}

internal fun CommonBackendContext.createArrayOfExpression(arrayElementType: KotlinType,
                                             arrayElements: List<IrExpression>,
                                             startOffset: Int, endOffset: Int): IrExpression {

    val genericArrayOfFunSymbol = ir.symbols.arrayOf
    val genericArrayOfFun = genericArrayOfFunSymbol.descriptor
    val typeParameter0 = genericArrayOfFun.typeParameters[0]
    val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType)))
    val substitutedArrayOfFun = genericArrayOfFun.substitute(typeSubstitutor)!!

    val typeArguments = mapOf(typeParameter0 to arrayElementType)

    val valueParameter0 = substitutedArrayOfFun.valueParameters[0]
    val arg0VarargType = valueParameter0.type
    val arg0VarargElementType = valueParameter0.varargElementType!!
    val arg0 = IrVarargImpl(startOffset, endOffset, arg0VarargType, arg0VarargElementType, arrayElements)

    return IrCallImpl(startOffset, endOffset, genericArrayOfFunSymbol, substitutedArrayOfFun, typeArguments).apply {
        putValueArgument(0, arg0)
    }
}
