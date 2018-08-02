/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.types.IrType

interface IrFunction : IrDeclarationWithVisibility, IrTypeParametersContainer, IrSymbolOwner, IrDeclarationParent, IrReturnTarget {
    override val descriptor: FunctionDescriptor
    override val symbol: IrFunctionSymbol

    val name: Name
    val isInline: Boolean // NB: there's an inline constructor for Array and each primitive array class
    val isExternal: Boolean
    var returnType: IrType

    var dispatchReceiverParameter: IrValueParameter?
    var extensionReceiverParameter: IrValueParameter?
    val valueParameters: MutableList<IrValueParameter>

    var body: IrBody?
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
