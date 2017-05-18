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

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrBindableSymbolBase
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

//-----------------------------------------------------------------------------//

interface IrReturnableBlockSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock>

interface IrReturnableBlock: IrBlock, IrSymbolOwner {
    override val symbol: IrReturnableBlockSymbol
    val descriptor: FunctionDescriptor
}

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor) :
        IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor),
        IrReturnableBlockSymbol

class IrReturnableBlockImpl(startOffset: Int, endOffset: Int, type: KotlinType,
                            override val symbol: IrReturnableBlockSymbol, origin: IrStatementOrigin? = null)
    : IrContainerExpressionBase(startOffset, endOffset, type, origin), IrReturnableBlock {

    override val descriptor = symbol.descriptor

    constructor(startOffset: Int, endOffset: Int, type: KotlinType,
                descriptor: FunctionDescriptor, origin: IrStatementOrigin? = null) :
            this(startOffset, endOffset, type, IrReturnableBlockSymbolImpl(descriptor), origin)

    constructor(startOffset: Int, endOffset: Int, type: KotlinType,
                descriptor: FunctionDescriptor, origin: IrStatementOrigin?, statements: List<IrStatement>) :
        this(startOffset, endOffset, type, descriptor, origin) {
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

//-----------------------------------------------------------------------------//

interface IrSuspensionPoint : IrExpression {
    var suspensionPointIdParameter: IrVariable
    var result: IrExpression
    var resumeResult: IrExpression
}

interface IrSuspendableExpression : IrExpression {
    var suspensionPointId: IrExpression
    var result: IrExpression
}

class IrSuspensionPointImpl(startOffset: Int, endOffset: Int, type: KotlinType,
                            override var suspensionPointIdParameter: IrVariable,
                            override var result: IrExpression,
                            override var resumeResult: IrExpression)
    : IrExpressionBase(startOffset, endOffset, type), IrSuspensionPoint {

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointIdParameter.accept(visitor, data)
        result.accept(visitor, data)
        resumeResult.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        suspensionPointIdParameter = suspensionPointIdParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
        resumeResult = resumeResult.transform(transformer, data)
    }
}

class IrSuspendableExpressionImpl(startOffset: Int, endOffset: Int, type: KotlinType,
                                  override var suspensionPointId: IrExpression, override var result: IrExpression)
    : IrExpressionBase(startOffset, endOffset, type), IrSuspendableExpression {

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointId.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        suspensionPointId = suspensionPointId.transform(transformer, data)
        result = result.transform(transformer, data)
    }
}