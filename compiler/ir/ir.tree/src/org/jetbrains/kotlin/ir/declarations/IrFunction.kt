/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

interface IrFunction :
    IrDeclarationWithName, IrDeclarationWithVisibility, IrTypeParametersContainer, IrSymbolOwner, IrDeclarationParent, IrReturnTarget {

    override val descriptor: FunctionDescriptor
    override val symbol: IrFunctionSymbol

    val isInline: Boolean // NB: there's an inline constructor for Array and each primitive array class
    val isExternal: Boolean
    val isExpect: Boolean

    var returnType: IrType

    var dispatchReceiverParameter: IrValueParameter?
    var extensionReceiverParameter: IrValueParameter?
    val valueParameters: MutableList<IrValueParameter>

    var body: IrBody?

    override var metadata: MetadataSource?
}


fun IrFunction.getIrValueParameter(parameter: ValueParameterDescriptor): IrValueParameter =
    valueParameters.getOrElse(parameter.index) {
        throw AssertionError("No IrValueParameter for $parameter")
    }.also { found ->
        assert(found.descriptor == parameter) {
            "Parameter indices mismatch at $descriptor: $parameter != ${found.descriptor}"
        }
    }

fun IrFunction.getDefault(parameter: ValueParameterDescriptor): IrExpressionBody? =
    getIrValueParameter(parameter).defaultValue

fun IrFunction.putDefault(parameter: ValueParameterDescriptor, expressionBody: IrExpressionBody) {
    getIrValueParameter(parameter).defaultValue = expressionBody
}

val IrFunction.isStaticMethodOfClass: Boolean
    get() = this is IrSimpleFunction && parent is IrClass && dispatchReceiverParameter == null
