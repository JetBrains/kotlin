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
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal


class IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrFieldSymbol,
    override val name: Name,
    override val type: IrType,
    override val visibility: Visibility,
    override val isFinal: Boolean,
    override val isExternal: Boolean,
    override val isStatic: Boolean
) : IrDeclarationBase(startOffset, endOffset, origin),
    IrField {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrFieldSymbol,
        type: IrType
    ) :
            this(
                startOffset, endOffset, origin, symbol,
                symbol.descriptor.name, type, symbol.descriptor.visibility,
                isFinal = !symbol.descriptor.isVar,
                isExternal = symbol.descriptor.isEffectivelyExternal(),
                isStatic = symbol.descriptor.dispatchReceiverParameter == null
            )

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType
    ) :
            this(startOffset, endOffset, origin, IrFieldSymbolImpl(descriptor), type)

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        initializer: IrExpressionBody?
    ) : this(startOffset, endOffset, origin, descriptor, type) {
        this.initializer = initializer
    }

    init {
        symbol.bind(this)
    }

    override val descriptor: PropertyDescriptor = symbol.descriptor

    override var initializer: IrExpressionBody? = null
    override var correspondingProperty: IrProperty? = null
    override val overriddenSymbols: MutableList<IrFieldSymbol> = mutableListOf()

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