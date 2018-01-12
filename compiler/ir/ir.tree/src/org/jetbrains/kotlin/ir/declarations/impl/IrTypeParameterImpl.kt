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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrTypeParameterSymbol,
    override val name: Name,
    override val index: Int,
    override val variance: Variance,
    override val upperBounds: List<KotlinType>
) : IrDeclarationBase(startOffset, endOffset, origin), IrTypeParameter {

    constructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, symbol: IrTypeParameterSymbol) :
            this(
                startOffset, endOffset, origin, symbol,
                symbol.descriptor.name,
                symbol.descriptor.index,
                symbol.descriptor.variance,
                symbol.descriptor.upperBounds.toMutableList()
            )

    constructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: TypeParameterDescriptor) :
            this(startOffset, endOffset, origin, IrTypeParameterSymbolImpl(descriptor))

    init {
        symbol.bind(this)
    }

    override val descriptor: TypeParameterDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrTypeParameter =
        transformer.visitTypeParameter(this, data) as IrTypeParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // no children
    }
}