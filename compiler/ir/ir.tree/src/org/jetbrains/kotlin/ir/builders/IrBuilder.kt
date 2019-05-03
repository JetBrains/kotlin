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

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

abstract class IrBuilder(
    override val context: IrGeneratorContext,
    var startOffset: Int,
    var endOffset: Int
) : IrGenerator

abstract class IrBuilderWithScope(
    context: IrGeneratorContext,
    override val scope: Scope,
    startOffset: Int,
    endOffset: Int
) : IrBuilder(context, startOffset, endOffset), IrGeneratorWithScope

abstract class IrStatementsBuilder<out T : IrElement>(
    context: IrGeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int
) : IrBuilderWithScope(context, scope, startOffset, endOffset), IrGeneratorWithScope {
    operator fun IrStatement.unaryPlus() {
        addStatement(this)
    }

    protected abstract fun addStatement(irStatement: IrStatement)
    abstract fun doBuild(): T
}

open class IrBlockBodyBuilder(
    context: IrGeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int
) : IrStatementsBuilder<IrBlockBody>(context, scope, startOffset, endOffset) {
    private val irBlockBody = IrBlockBodyImpl(startOffset, endOffset)

    inline fun blockBody(body: IrBlockBodyBuilder.() -> Unit): IrBlockBody {
        body()
        return doBuild()
    }

    override fun addStatement(irStatement: IrStatement) {
        irBlockBody.statements.add(irStatement)
    }

    override fun doBuild(): IrBlockBody {
        return irBlockBody
    }
}

class IrBlockBuilder(
    context: IrGeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int,
    val origin: IrStatementOrigin? = null,
    var resultType: IrType? = null,
    val isTransparent:Boolean = false
) : IrStatementsBuilder<IrContainerExpression>(context, scope, startOffset, endOffset) {

    private val statements = ArrayList<IrStatement>()

    inline fun block(body: IrBlockBuilder.() -> Unit): IrContainerExpression {
        body()
        return doBuild()
    }

    override fun addStatement(irStatement: IrStatement) {
        statements.add(irStatement)
    }

    override fun doBuild(): IrContainerExpression {
        val resultType = this.resultType
                ?: statements.lastOrNull().safeAs<IrExpression>()?.type
                ?: context.irBuiltIns.unitType
        val irBlock = if (isTransparent) IrCompositeImpl(startOffset, endOffset, resultType, origin) else IrBlockImpl(startOffset, endOffset, resultType, origin)
        irBlock.statements.addAll(statements)
        return irBlock
    }
}

class IrSingleStatementBuilder(
    context: IrGeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int,
    val origin: IrStatementOrigin? = null
) : IrBuilderWithScope(context, scope, startOffset, endOffset) {

    inline fun <T : IrElement> build(statementBuilder: IrSingleStatementBuilder.() -> T): T =
        statementBuilder()
}

inline fun <T : IrElement> IrGeneratorWithScope.buildStatement(
    startOffset: Int,
    endOffset: Int,
    origin: IrStatementOrigin?,
    builder: IrSingleStatementBuilder.() -> T
) =
    IrSingleStatementBuilder(context, scope, startOffset, endOffset, origin).builder()

inline fun <T : IrElement> IrGeneratorWithScope.buildStatement(
    startOffset: Int,
    endOffset: Int,
    builder: IrSingleStatementBuilder.() -> T
) =
    IrSingleStatementBuilder(context, scope, startOffset, endOffset).builder()

fun <T : IrBuilder> T.at(startOffset: Int, endOffset: Int) = apply {
    this.startOffset = startOffset
    this.endOffset = endOffset
}

inline fun IrGeneratorWithScope.irBlock(
    startOffset: Int, endOffset: Int,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType
    ).block(body)

inline fun IrGeneratorWithScope.irComposite(
    startOffset: Int, endOffset: Int,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType, true
    ).block(body)

inline fun IrGeneratorWithScope.irBlockBody(
    startOffset: Int, endOffset: Int,
    body: IrBlockBodyBuilder.() -> Unit
): IrBlockBody =
    IrBlockBodyBuilder(
        context, scope,
        startOffset,
        endOffset
    ).blockBody(body)

