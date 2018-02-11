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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType


class IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrFieldSymbol,
    override val name: Name,
    override val type: KotlinType,
    override val visibility: Visibility
) : IrDeclarationBase(startOffset, endOffset, origin), IrField {
    constructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, symbol: IrFieldSymbol) :
            this(
                startOffset, endOffset, origin, symbol,
                symbol.descriptor.name, symbol.descriptor.type, symbol.descriptor.visibility
            )

    constructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: PropertyDescriptor) :
            this(startOffset, endOffset, origin, IrFieldSymbolImpl(descriptor))

    constructor(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: PropertyDescriptor,
        initializer: IrExpressionBody?
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.initializer = initializer
    }

    init {
        symbol.bind(this)
    }

    override val descriptor: PropertyDescriptor = symbol.descriptor

    override var initializer: IrExpressionBody? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitField(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }
}