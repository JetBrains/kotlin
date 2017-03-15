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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.SmartList

abstract class IrFunctionBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin
) : IrDeclarationBase(startOffset, endOffset, origin), IrFunction {
    override val typeParameters: MutableList<IrTypeParameter> = SmartList()

    override val valueParameters: MutableList<IrValueParameter> = ArrayList()

    final override var body: IrBody? = null

    private fun getIrValueParameter(parameter: ValueParameterDescriptor): IrValueParameter =
            valueParameters.getOrElse(parameter.index) {
                throw AssertionError("No IrValueParameter for $parameter")
            }

    override fun getDefault(parameter: ValueParameterDescriptor): IrExpressionBody? =
            getIrValueParameter(parameter).defaultValue

    override fun putDefault(parameter: ValueParameterDescriptor, expressionBody: IrExpressionBody) {
        getIrValueParameter(parameter).defaultValue = expressionBody
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters.transform { it.transform(transformer, data) }
        valueParameters.transform { it.transform(transformer, data) }

        body = body?.transform(transformer, data)
    }
}