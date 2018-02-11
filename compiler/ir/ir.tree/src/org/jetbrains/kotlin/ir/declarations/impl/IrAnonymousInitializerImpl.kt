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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrAnonymousInitializerImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrAnonymousInitializerSymbol
) : IrDeclarationBase(startOffset, endOffset, origin), IrAnonymousInitializer {
    constructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor) :
            this(startOffset, endOffset, origin, IrAnonymousInitializerSymbolImpl(descriptor))

    constructor(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        body: IrBlockBody
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.body = body
    }

    init {
        symbol.bind(this)
    }

    override val descriptor: ClassDescriptor get() = symbol.descriptor

    override lateinit var body: IrBlockBody

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousInitializer(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        body = body.transform(transformer, data) as IrBlockBody
    }
}