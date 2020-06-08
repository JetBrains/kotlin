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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.ConstructorCarrier
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrConstructorImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    name: Name,
    visibility: Visibility,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    override val isPrimary: Boolean,
    isExpect: Boolean
) :
    IrFunctionBase<ConstructorCarrier>(
        startOffset, endOffset, origin, name,
        visibility,
        isInline, isExternal, isExpect,
        returnType
    ),
    IrConstructor,
    ConstructorCarrier {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrConstructorSymbol,
        returnType: IrType,
        descriptor: ClassConstructorDescriptor,
        name: Name = descriptor.name
    ) : this(
        startOffset, endOffset, origin, symbol,
        name = name, visibility = descriptor.visibility,
        returnType = returnType,
        isInline = descriptor.isInline,
        isExternal = descriptor.isEffectivelyExternal(),
        isPrimary = descriptor.isPrimary,
        isExpect = descriptor.isExpect
    )

    init {
        symbol.bind(this)
    }

    @DescriptorBasedIr
    override val descriptor: ClassConstructorDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)
}
