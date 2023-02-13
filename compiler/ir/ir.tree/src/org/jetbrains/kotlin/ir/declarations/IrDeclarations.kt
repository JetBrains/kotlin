/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.IrSimpleType
import java.io.File

fun <D : IrAttributeContainer> D.copyAttributes(other: IrAttributeContainer?): D = apply {
    if (other != null) {
        attributeOwnerId = other.attributeOwnerId
        originalBeforeInline = other.originalBeforeInline
    }
}

val IrClass.isSingleFieldValueClass: Boolean
    get() = valueClassRepresentation is InlineClassRepresentation

val IrClass.isMultiFieldValueClass: Boolean
    get() = valueClassRepresentation is MultiFieldValueClassRepresentation

fun IrClass.addMember(member: IrDeclaration) {
    declarations.add(member)
}

fun IrClass.addAll(members: List<IrDeclaration>) {
    declarations.addAll(members)
}

val IrFile.path: String get() = fileEntry.name
val IrFile.name: String get() = File(path).name

@ObsoleteDescriptorBasedAPI
fun IrFunction.getIrValueParameter(parameter: ValueParameterDescriptor): IrValueParameter =
    getIrValueParameter(parameter, parameter.index)

@ObsoleteDescriptorBasedAPI
fun IrFunction.getIrValueParameter(parameter: ParameterDescriptor, index: Int): IrValueParameter =
    valueParameters.getOrElse(index) {
        throw AssertionError("No IrValueParameter for $parameter")
    }.also { found ->
        assert(found.descriptor == parameter) {
            "Parameter indices mismatch at $descriptor: $parameter != ${found.descriptor}"
        }
    }

@ObsoleteDescriptorBasedAPI
fun IrFunction.putDefault(parameter: ValueParameterDescriptor, expressionBody: IrExpressionBody) {
    getIrValueParameter(parameter).defaultValue = expressionBody
}

val IrFunction.isStaticMethodOfClass: Boolean
    get() = this is IrSimpleFunction && parent is IrClass && dispatchReceiverParameter == null

val IrFunction.isPropertyAccessor: Boolean
    get() = this is IrSimpleFunction && correspondingPropertySymbol != null


val IrClass.multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<IrSimpleType>?
    get() = valueClassRepresentation as? MultiFieldValueClassRepresentation<IrSimpleType>

val IrClass.inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>?
    get() = valueClassRepresentation as? InlineClassRepresentation<IrSimpleType>
