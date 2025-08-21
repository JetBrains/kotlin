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
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.name.Name

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

    operator fun List<IrStatement>.unaryPlus() {
        forEach(::addStatement)
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
    private val irBlockBody = context.irFactory.createBlockBody(startOffset, endOffset)

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

abstract class IrAbstractBlockBuilder<T : IrContainerExpression>(
    context: IrGeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int,
    val origin: IrStatementOrigin?,
    var resultType: IrType?,
) : IrStatementsBuilder<T>(context, scope, startOffset, endOffset) {
    private val statements = ArrayList<IrStatement>()

    override fun addStatement(irStatement: IrStatement) {
        statements.add(irStatement)
    }

    abstract fun createBlock(resultType: IrType, origin: IrStatementOrigin?): T

    override fun doBuild(): T {
        val resultType = this.resultType
            ?: (statements.lastOrNull() as? IrExpression)?.type
            ?: context.irBuiltIns.unitType
        val irBlock = createBlock(resultType, origin)
        irBlock.statements.addAll(statements)
        return irBlock
    }
}

class IrBlockBuilder(
    context: IrGeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    val isTransparent: Boolean = false
) : IrAbstractBlockBuilder<IrContainerExpression>(context, scope, startOffset, endOffset, origin, resultType) {
    override fun createBlock(resultType: IrType, origin: IrStatementOrigin?): IrContainerExpression {
        return if (isTransparent) {
            IrCompositeImpl(startOffset, endOffset, resultType, origin)
        } else {
            IrBlockImpl(startOffset, endOffset, resultType, origin)
        }
    }

    inline fun block(body: IrBlockBuilder.() -> Unit): IrContainerExpression {
        body()
        return doBuild()
    }
}

class IrReturnableBlockBuilder(
    context: IrGeneratorContext,
    scope: Scope,
    val blockStartOffset: Int,
    val blockEndOffset: Int,
    resultType: IrType,
    origin: IrStatementOrigin? = null,
) : IrAbstractBlockBuilder<IrReturnableBlock>(context, scope, blockStartOffset, blockEndOffset, origin, resultType) {
    val returnableBlockSymbol: IrReturnableBlockSymbol = IrReturnableBlockSymbolImpl()

    override fun createBlock(resultType: IrType, origin: IrStatementOrigin?): IrReturnableBlock {
        return IrReturnableBlockImpl(
            startOffset = blockStartOffset,
            endOffset = blockEndOffset,
            type = resultType,
            symbol = returnableBlockSymbol,
            origin = null,
        )
    }
}

class IrInlinedFunctionBlockBuilder(
    context: IrGeneratorContext,
    scope: Scope,
    val blockStartOffset: Int,
    val blockEndOffset: Int,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    val inlinedFunctionStartOffset: Int,
    val inlinedFunctionEndOffset: Int,
    val inlinedFunctionSymbol: IrFunctionSymbol?,
    val inlinedFunctionFileEntry: IrFileEntry,
) : IrAbstractBlockBuilder<IrInlinedFunctionBlock>(context, scope, blockStartOffset, blockEndOffset, origin, resultType) {
    override fun createBlock(resultType: IrType, origin: IrStatementOrigin?): IrInlinedFunctionBlock {
        return IrInlinedFunctionBlockImpl(
            startOffset = blockStartOffset,
            endOffset = blockEndOffset,
            inlinedFunctionStartOffset = inlinedFunctionStartOffset,
            inlinedFunctionEndOffset = inlinedFunctionEndOffset,
            type = resultType,
            inlinedFunctionSymbol = inlinedFunctionSymbol,
            inlinedFunctionFileEntry = inlinedFunctionFileEntry,
            origin = origin,
        )
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

inline fun IrBuilderWithScope.irReturnableBlock(
    resultType: IrType,
    origin: IrStatementOrigin? = null,
    body: IrReturnableBlockBuilder.() -> Unit
): IrReturnableBlock =
    IrReturnableBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        resultType = resultType,
        origin = origin
    ).apply {
        body()
    }.doBuild()

inline fun IrBuilderWithScope.irInlinedFunctionBlock(
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    inlinedFunctionStartOffset: Int,
    inlinedFunctionEndOffset: Int,
    inlinedFunctionSymbol: IrFunctionSymbol?,
    inlinedFunctionFileEntry: IrFileEntry,
    body: IrInlinedFunctionBlockBuilder.() -> Unit
): IrInlinedFunctionBlock =
    IrInlinedFunctionBlockBuilder(
        context,
        scope,
        startOffset, endOffset,
        resultType = resultType,
        origin = origin,
        inlinedFunctionEndOffset = inlinedFunctionEndOffset,
        inlinedFunctionStartOffset = inlinedFunctionStartOffset,
        inlinedFunctionSymbol = inlinedFunctionSymbol,
        inlinedFunctionFileEntry = inlinedFunctionFileEntry,
    ).apply {
        body()
    }.doBuild()

fun IrBuilderWithScope.irWhile(origin: IrStatementOrigin? = null) =
    IrWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, origin)

fun IrBuilderWithScope.irDoWhile(origin: IrStatementOrigin? = null) =
    IrDoWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, origin)

fun IrBuilderWithScope.irBreak(loop: IrLoop) =
    IrBreakImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)

fun IrBuilderWithScope.irContinue(loop: IrLoop) =
    IrContinueImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)

fun IrBuilderWithScope.irGetObject(classSymbol: IrClassSymbol) =
    IrGetObjectValueImpl(startOffset, endOffset, IrSimpleTypeImpl(classSymbol, false, emptyList(), emptyList()), classSymbol)

// Also adds created variable into building block
fun <T : IrElement> IrStatementsBuilder<T>.createTmpVariable(
    irExpression: IrExpression,
    nameHint: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    irType: IrType? = null
): IrVariable {
    val variable = scope.createTemporaryVariable(irExpression, nameHint, isMutable, origin, irType, inventUniqueName = false)
    +variable
    return variable
}
