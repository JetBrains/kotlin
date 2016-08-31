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

package org.jetbrains.kotlin.psi2ir.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.Generator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorWithScope
import org.jetbrains.kotlin.psi2ir.generators.Scope
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

abstract class IrBuilder(
        override val context: GeneratorContext,
        var startOffset: Int,
        var endOffset: Int
) : Generator

abstract class IrBuilderWithScope(
        context: GeneratorContext,
        override val scope: Scope,
        startOffset: Int,
        endOffset: Int
) : IrBuilder(context, startOffset, endOffset), GeneratorWithScope

abstract class IrStatementsBuilder<out T : IrElement>(
        context: GeneratorContext,
        scope: Scope,
        startOffset: Int,
        endOffset: Int
) : IrBuilderWithScope(context, scope, startOffset, endOffset), GeneratorWithScope {
    operator fun IrStatement.unaryPlus() {
        addStatement(this)
    }

    protected abstract fun addStatement(irStatement: IrStatement)
    protected abstract fun doBuild(): T
}

open class IrBlockBodyBuilder(
        context: GeneratorContext,
        scope: Scope,
        startOffset: Int,
        endOffset: Int
) : IrStatementsBuilder<IrBlockBody>(context, scope, startOffset, endOffset) {
    private val irBlockBody = IrBlockBodyImpl(startOffset, endOffset)

    override fun addStatement(irStatement: IrStatement) {
        irBlockBody.addStatement(irStatement)
    }

    override fun doBuild(): IrBlockBody {
        return irBlockBody
    }
}

class IrBlockBuilder(
        context: GeneratorContext,
        scope: Scope,
        startOffset: Int,
        endOffset: Int,
        val operator: IrOperator? = null,
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
        val resultType = this.resultType ?:
                         (statements.lastOrNull() as? IrExpression)?.type ?:
                         context.builtIns.unitType
        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, operator)
        irBlock.addAll(statements)
        return irBlock
    }
}

fun <T : IrBuilder> T.at(startOffset: Int, endOffset: Int): T {
    this.startOffset = startOffset
    this.endOffset = endOffset
    return this
}

inline fun GeneratorWithScope.irBlock(ktElement: KtElement? = null, operator: IrOperator? = null, resultType: KotlinType? = null,
                                      body: IrBlockBuilder.() -> Unit
): IrExpression =
        IrBlockBuilder(context, scope,
                       ktElement?.startOffset ?: UNDEFINED_OFFSET,
                       ktElement?.endOffset ?: UNDEFINED_OFFSET,
                       operator, resultType
        ).block(body)