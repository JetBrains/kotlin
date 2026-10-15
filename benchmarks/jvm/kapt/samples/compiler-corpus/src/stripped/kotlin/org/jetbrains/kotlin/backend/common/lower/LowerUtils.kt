/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.linkage.partial.isPartialLinkageRuntimeError
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class DeclarationIrBuilder(
    generatorContext: IrGeneratorContext,
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET
) : IrBuilderWithScope(
    generatorContext,
    Scope(symbol),
    startOffset,
    endOffset
)

abstract class AbstractVariableRemapper : IrElementTransformerVoid() {
    protected abstract fun remapVariable(value: IrValueDeclaration): IrValueDeclaration?

    override fun visitGetValue(expression: IrGetValue): IrExpression = TODO()

    override fun visitSetValue(expression: IrSetValue): IrExpression { TODO() }
}

open class VariableRemapper(val mapping: Map<IrValueParameter, IrValueDeclaration>) : AbstractVariableRemapper() {
    override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? = TODO()
}

fun IrBuiltIns.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) =
    DeclarationIrBuilder(IrGeneratorContextBase(this), symbol, startOffset, endOffset)

fun LoweringContext.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) =
    irBuiltIns.createIrBuilder(symbol, startOffset, endOffset)


fun <T : IrBuilder> T.at(element: IrElement) = this.at(element.startOffset, element.endOffset)

/**
 * Builds [IrBlock] to be used instead of given expression.
 */
inline fun IrGeneratorWithScope.irBlock(
    expression: IrExpression, origin: IrStatementOrigin? = null,
    resultType: IrType? = expression.type,
    startOffset: Int = expression.startOffset,
    endOffset: Int = expression.endOffset,
    body: IrBlockBuilder.() -> Unit
) =
    this.irBlock(startOffset, endOffset, origin, resultType, body)

inline fun IrGeneratorWithScope.irComposite(
    expression: IrExpression, origin: IrStatementOrigin? = null,
    resultType: IrType? = expression.type,
    body: IrBlockBuilder.() -> Unit
) =
    this.irComposite(expression.startOffset, expression.endOffset, origin, resultType, body)

inline fun IrGeneratorWithScope.irBlockBody(irElement: IrElement, body: IrBlockBodyBuilder.() -> Unit) =
    this.irBlockBody(irElement.startOffset, irElement.endOffset, body)

fun IrBuilderWithScope.irIfThen(condition: IrExpression, thenPart: IrExpression) =
    IrWhenImpl(startOffset, endOffset, context.irBuiltIns.unitType).apply {
        branches += IrBranchImpl(condition, thenPart)
    }

fun IrBuilderWithScope.irNot(arg: IrExpression) =
    primitiveOp1(startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, context.irBuiltIns.booleanType, IrStatementOrigin.EXCL, arg)

fun IrBuilderWithScope.irThrow(arg: IrExpression) =
    IrThrowImpl(startOffset, endOffset, context.irBuiltIns.nothingType, arg)

fun IrBuilderWithScope.irCatch(catchParameter: IrVariable, result: IrExpression, origin: IrStatementOrigin? = null): IrCatch = TODO()

fun IrBuilderWithScope.irImplicitCoercionToUnit(arg: IrExpression) =
    IrTypeOperatorCallImpl(
        startOffset, endOffset, context.irBuiltIns.unitType,
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, context.irBuiltIns.unitType,
        arg
    )

open class IrBuildingTransformer(private val context: LoweringContext) : IrElementTransformerVoid() {
    private var currentBuilder: IrBuilderWithScope? = null

    protected val builder: IrBuilderWithScope
        get() = currentBuilder!!

    private inline fun <T> withBuilder(symbol: IrSymbol, block: () -> T): T {
        val oldBuilder = currentBuilder
        currentBuilder = context.createIrBuilder(symbol)
        return try {
            block()
        } finally {
            currentBuilder = oldBuilder
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement { TODO() }

    override fun visitField(declaration: IrField): IrStatement { TODO() }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement { TODO() }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement { TODO() }

    override fun visitScript(declaration: IrScript): IrStatement { TODO() }
}

enum class ConstructorDelegationKind {
    /** Calls another constructor of the same class. */
    CALLS_THIS,

    /** Calls the constructor of the super class. */
    CALLS_SUPER,

    /** The actual delegation is not known. The constructor call was replaced by a partial linkage error. */
    PARTIAL_LINKAGE_ERROR
}

fun IrConstructor.delegationKind(context: LoweringContext): ConstructorDelegationKind { TODO() }
