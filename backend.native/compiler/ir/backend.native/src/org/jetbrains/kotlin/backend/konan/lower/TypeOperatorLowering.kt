/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.backend.konan.ir.containsNull
import org.jetbrains.kotlin.backend.konan.ir.isSubtypeOf
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

/**
 * This lowering pass lowers some [IrTypeOperatorCall]s.
 */
internal class TypeOperatorLowering(val context: CommonBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        val transformer = TypeOperatorTransformer(context, irFunction.symbol)
        irFunction.transformChildrenVoid(transformer)
    }
}


private class TypeOperatorTransformer(val context: CommonBackendContext, val function: IrFunctionSymbol) : IrElementTransformerVoid() {

    private val builder = context.createIrBuilder(function)

    val throwNullPointerException = context.ir.symbols.throwNullPointerException

    override fun visitFunction(declaration: IrFunction): IrStatement {
        // ignore inner functions during this pass
        return declaration
    }

    private fun IrType.erasure(): IrType {
        if (this !is IrSimpleType) return this

        val classifier = this.classifier
        return when (classifier) {
            is IrClassSymbol -> this
            is IrTypeParameterSymbol -> {
                val upperBound = classifier.owner.superTypes.firstOrNull() ?:
                        TODO("${classifier.descriptor} : ${classifier.descriptor.upperBounds}")

                if (this.hasQuestionMark) {
                    // `T?`
                    upperBound.erasure().makeNullable()
                } else {
                    upperBound.erasure()
                }
            }
            else -> TODO(classifier.toString())
        }
    }

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        builder.at(expression)
        val typeOperand = expression.typeOperand.erasure()

//        assert (!TypeUtils.hasNullableSuperType(typeOperand)) // So that `isNullable()` <=> `isMarkedNullable`.

        // TODO: consider the case when expression type is wrong e.g. due to generics-related unchecked casts.

        return when {
            expression.argument.type.isSubtypeOf(typeOperand) -> expression.argument

            expression.argument.type.containsNull() -> {
                with (builder) {
                    irLetS(expression.argument) { argument ->
                        irIfThenElse(
                                type = expression.type,
                                condition = irEqeqeq(irGet(argument.owner), irNull()),

                                thenPart = if (typeOperand.isSimpleTypeWithQuestionMark)
                                    irNull()
                                else
                                    irCall(throwNullPointerException.owner),

                                elsePart = irAs(irGet(argument.owner), typeOperand.makeNotNull())
                        )
                    }
                }
            }

            typeOperand.isSimpleTypeWithQuestionMark -> builder.irAs(expression.argument, typeOperand.makeNotNull())

            typeOperand == expression.typeOperand -> expression

            else -> builder.irAs(expression.argument, typeOperand)
        }
    }

    private fun KotlinType.isNullable() = TypeUtils.isNullableType(this)

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = expression.typeOperand.erasure()

        return builder.irBlock(expression) {
            +irLetS(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irIs(irGet(variable.owner), typeOperand),
                        thenPart = irImplicitCast(irGet(variable.owner), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            else -> expression
        }
    }
}