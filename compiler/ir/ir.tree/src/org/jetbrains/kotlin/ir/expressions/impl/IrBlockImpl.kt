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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

class IrBlockImpl(startOffset: Int, endOffset: Int, type: KotlinType, origin: IrStatementOrigin? = null) :
    IrContainerExpressionBase(startOffset, endOffset, type, origin), IrBlock {
    constructor(startOffset: Int, endOffset: Int, type: KotlinType, origin: IrStatementOrigin?, statements: List<IrStatement>) :
            this(startOffset, endOffset, type, origin) {
        this.statements.addAll(statements)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)
}

fun IrBlockImpl.addIfNotNull(statement: IrStatement?) {
    if (statement != null) statements.add(statement)
}

fun IrBlockImpl.inlineStatement(statement: IrStatement) {
    if (statement is IrBlock) {
        statements.addAll(statement.statements)
    } else {
        statements.add(statement)
    }
}


class IrReturnableBlockImpl(
    startOffset: Int, endOffset: Int, type: KotlinType,
    override val symbol: IrReturnableBlockSymbol, origin: IrStatementOrigin? = null, override val sourceFileName: String = "no source file"
) : IrContainerExpressionBase(startOffset, endOffset, type, origin), IrReturnableBlock {
    override val descriptor = symbol.descriptor

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        symbol: IrReturnableBlockSymbol,
        origin: IrStatementOrigin?,
        statements: List<IrStatement>,
        sourceFileName: String = "no source file"
    ) :
            this(startOffset, endOffset, type, symbol, origin, sourceFileName) {
        this.statements.addAll(statements)
    }

    constructor(
        startOffset: Int, endOffset: Int, type: KotlinType,
        descriptor: FunctionDescriptor, origin: IrStatementOrigin? = null, sourceFileName: String = "no source file"
    ) :
            this(startOffset, endOffset, type, IrReturnableBlockSymbolImpl(descriptor), origin, sourceFileName)

    constructor(
        startOffset: Int, endOffset: Int, type: KotlinType,
        descriptor: FunctionDescriptor, origin: IrStatementOrigin?, statements: List<IrStatement>, sourceFileName: String = "no source file"
    ) :
            this(startOffset, endOffset, type, descriptor, origin, sourceFileName) {
        this.statements.addAll(statements)
    }

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        statements.forEachIndexed { i, irStatement ->
            statements[i] = irStatement.transform(transformer, data)
        }
    }
}