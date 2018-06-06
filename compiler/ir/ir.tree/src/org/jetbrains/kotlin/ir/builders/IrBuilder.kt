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
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.types.KotlinType
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
    context: IrGeneratorContext, scope: Scope,
    startOffset: Int, endOffset: Int,
    val origin: IrStatementOrigin? = null,
    var resultType: KotlinType? = null
) : IrStatementsBuilder<IrBlock>(context, scope, startOffset, endOffset) {
    private val statements = ArrayList<IrStatement>()

    inline fun block(body: IrBlockBuilder.() -> Unit): IrBlock {
        body()
        return doBuild()
    }

    override fun addStatement(irStatement: IrStatement) {
        statements.add(irStatement)
    }

    override fun doBuild(): IrBlock {
        val resultType = this.resultType ?: (statements.lastOrNull() as? IrExpression)?.type ?: context.builtIns.unitType
        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, origin)
        irBlock.statements.addAll(statements)
        return irBlock
    }
}

fun <T : IrBuilder> T.at(startOffset: Int, endOffset: Int): T {
    this.startOffset = startOffset
    this.endOffset = endOffset
    return this
}

inline fun IrGeneratorWithScope.irBlock(
    startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET,
    origin: IrStatementOrigin? = null, resultType: KotlinType? = null,
    body: IrBlockBuilder.() -> Unit
): IrExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType
    ).block(body)

inline fun IrGeneratorWithScope.irBlockBody(
    startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET,
    body: IrBlockBodyBuilder.() -> Unit
): IrBlockBody =
    IrBlockBodyBuilder(
        context, scope,
        startOffset,
        endOffset
    ).blockBody(body)
