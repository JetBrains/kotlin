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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class IrFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    override val name: Name,
    visibility: Visibility,
    override val modality: Modality,
    returnType: KotlinType,
    isInline: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean
) :
    IrFunctionBase(startOffset, endOffset, origin, visibility, isInline, returnType),
    IrSimpleFunction {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrSimpleFunctionSymbol
    ) : this(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.visibility,
        symbol.descriptor.modality,
        symbol.descriptor.returnType!!,
        symbol.descriptor.isInline,
        symbol.descriptor.isTailrec,
        symbol.descriptor.isSuspend
    )

    override val descriptor: FunctionDescriptor = symbol.descriptor

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor
    ) : this(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(descriptor)
    )

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        body: IrBody?
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.body = body
    }

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}