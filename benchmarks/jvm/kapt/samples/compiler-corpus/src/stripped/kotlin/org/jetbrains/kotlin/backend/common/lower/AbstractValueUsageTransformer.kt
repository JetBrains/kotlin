/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.ir.util.innerInlinedBlockOrThis
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom


/**
 * Transforms expressions depending on the context they are used in.
 *
 * The transformations are defined with `IrExpression.use*` methods in this class,
 * the most common are [useAs], [useAsStatement], [useInTypeOperator].
 *
 * NOTE: Transformer is copied from Kotlin/Native with minor modifications
 *
 * TODO: the implementation is originally based on [org.jetbrains.kotlin.psi2ir.transformations.InsertImplicitCasts]
 * and should probably be used as its base.
 *
 * TODO: consider making this visitor non-recursive to make it more general.
 */
abstract class AbstractValueUsageTransformer(
    protected val irBuiltIns: IrBuiltIns,
    private val replaceTypesInsideInlinedFunctionBlock: Boolean = false
) : IrElementTransformerVoid() {

    protected open fun IrExpression.useAs(type: IrType): IrExpression = TODO()

    protected open fun IrExpression.useAsStatement(): IrExpression = TODO()

    protected open fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: IrType): IrExpression = TODO()

    protected open fun IrExpression.useAsValue(value: IrValueDeclaration): IrExpression = TODO()

    protected open fun IrExpression.useAsValueArgument(
        expression: IrFunctionAccessExpression,
        parameter: IrValueParameter
    ): IrExpression = TODO()

    private fun IrExpression.useForVariable(variable: IrVariable): IrExpression = TODO()

    private fun IrExpression.useForField(field: IrField): IrExpression = TODO()

    protected open fun IrExpression.useAsReturnValue(returnTarget: IrReturnTargetSymbol): IrExpression = TODO()

    protected open fun IrExpression.useAsResult(enclosing: IrExpression): IrExpression = TODO()

    protected open fun useAsVarargElement(element: IrExpression, expression: IrVararg): IrExpression = TODO()


    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression { TODO() }

    override fun visitBlockBody(body: IrBlockBody): IrBody { TODO() }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock): IrExpression { TODO() }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression { TODO() }

    override fun visitReturn(expression: IrReturn): IrExpression { TODO() }

    override fun visitSetValue(expression: IrSetValue): IrExpression { TODO() }

    override fun visitSetField(expression: IrSetField): IrExpression { TODO() }

    override fun visitField(declaration: IrField): IrStatement { TODO() }

    override fun visitVariable(declaration: IrVariable): IrVariable { TODO() }

    override fun visitWhen(expression: IrWhen): IrExpression { TODO() }

    override fun visitLoop(loop: IrLoop): IrExpression { TODO() }

    override fun visitThrow(expression: IrThrow): IrExpression { TODO() }

    override fun visitTry(aTry: IrTry): IrExpression { TODO() }

    override fun visitVararg(expression: IrVararg): IrExpression { TODO() }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression { TODO() }

    override fun visitFunction(declaration: IrFunction): IrStatement { TODO() }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression { TODO() }

    // TODO: IrEnumEntry?

}

