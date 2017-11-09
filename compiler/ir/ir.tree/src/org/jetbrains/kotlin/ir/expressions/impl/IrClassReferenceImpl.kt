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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

class IrClassReferenceImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        symbol: IrClassifierSymbol
) : IrClassReference,
        IrTerminalDeclarationReferenceBase<IrClassifierSymbol, ClassifierDescriptor>(
                startOffset, endOffset, type,
                symbol, symbol.descriptor
        )
{
    @Deprecated("Creates unbound symbols")
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            descriptor: ClassifierDescriptor
    ) : this(startOffset, endOffset, type, createClassifierSymbolForClassReference(descriptor))

    override val descriptor: ClassifierDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitClassReference(this, data)
}

internal fun createClassifierSymbolForClassReference(descriptor: ClassifierDescriptor): IrClassifierSymbol =
        when (descriptor) {
            is ClassDescriptor -> IrClassSymbolImpl(descriptor)
            is TypeParameterDescriptor -> IrTypeParameterSymbolImpl(descriptor)
            else -> throw IllegalArgumentException("Unexpected referenced classifier: $descriptor")
        }