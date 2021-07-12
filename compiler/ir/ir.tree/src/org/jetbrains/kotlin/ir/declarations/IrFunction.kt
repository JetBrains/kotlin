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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrFunction :
    IrDeclarationBase(),
    IrPossiblyExternalDeclaration, IrDeclarationWithVisibility, IrTypeParametersContainer, IrSymbolOwner, IrDeclarationParent, IrReturnTarget,
    IrMemberWithContainerSource,
    IrMetadataSourceOwner {

    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: FunctionDescriptor
    abstract override val symbol: IrFunctionSymbol

    abstract val isInline: Boolean // NB: there's an inline constructor for Array and each primitive array class
    abstract val isExpect: Boolean

    abstract var returnType: IrType

    abstract var dispatchReceiverParameter: IrValueParameter?
    abstract var extensionReceiverParameter: IrValueParameter?
    abstract var valueParameters: List<IrValueParameter>

    abstract var body: IrBody?

    var contextReceiverParametersCount: Int = 0

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }

        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }

        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)

        dispatchReceiverParameter = dispatchReceiverParameter?.transform(transformer, data)
        extensionReceiverParameter = extensionReceiverParameter?.transform(transformer, data)
        valueParameters = valueParameters.transformIfNeeded(transformer, data)

        body = body?.transform(transformer, data)
    }
}

@ObsoleteDescriptorBasedAPI
fun IrFunction.getIrValueParameter(parameter: ValueParameterDescriptor): IrValueParameter =
    valueParameters.getOrElse(parameter.index) {
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
