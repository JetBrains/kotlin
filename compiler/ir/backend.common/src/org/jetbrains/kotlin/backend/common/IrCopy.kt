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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SymbolRemapper

open class DeepCopyIrTreeWithReturnableBlockSymbols(
        private val symbolRemapper: SymbolRemapper
) : DeepCopyIrTreeWithSymbols(symbolRemapper) {

    private inline fun <reified T : IrElement> T.transform() =
            transform(this@DeepCopyIrTreeWithReturnableBlockSymbols, null) as T

    private val transformedReturnableBlocks = mutableMapOf<IrReturnableBlock, IrReturnableBlock>()

    override fun visitBlock(expression: IrBlock): IrBlock = if (expression is IrReturnableBlock) {
        IrReturnableBlockImpl(
                expression.startOffset, expression.endOffset,
                expression.type,
                expression.descriptor,
                expression.origin,
                expression.sourceFileName
        ).also {
            transformedReturnableBlocks.put(expression, it)
            it.statements.addAll(expression.statements.map { it.transform() })
        }
    } else {
        super.visitBlock(expression)
    }

    override fun visitReturn(expression: IrReturn): IrReturn {
        val returnTargetSymbol = expression.returnTargetSymbol
        return if (returnTargetSymbol is IrReturnableBlockSymbol) {
            IrReturnImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    transformedReturnableBlocks.getOrElse(returnTargetSymbol.owner) { returnTargetSymbol.owner }.symbol,
                    expression.value.transform()
            )
        } else {
            super.visitReturn(expression)
        }
    }
}