/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// After this pass runs there are only four kinds of IrTypeOperatorCalls left:
//
// - IMPLICIT_CAST
// - SAFE_CAST with reified type parameters
// - INSTANCEOF with non-nullable type operand or reified type parameters
// - CAST with non-nullable argument, nullable type operand, or reified type parameters
//
// The latter two correspond to the instanceof/checkcast instructions on the JVM, except for
// the presence of reified type parameters.
internal val typeOperatorLowering = makeIrFilePhase(
    ::TypeOperatorLowering,
    name = "TypeOperatorLowering",
    description = "Lower IrTypeOperatorCalls to (implicit) casts and instanceof checks"
)

private class TypeOperatorLowering(private val context: JvmBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrExpression.transformVoid() = transform(this@TypeOperatorLowering, null)

    private val IrType.isReifiedTypeParameter: Boolean
        get() = classifierOrNull?.safeAs<IrTypeParameterSymbol>()?.owner?.isReified == true

    private fun lowerInstanceOf(argument: IrExpression, type: IrType) = with(builder) {
        when {
            type.isReifiedTypeParameter ->
                irIs(argument, type)
            argument.type.isNullable() && type.isNullable() -> {
                irLetS(argument) { valueSymbol ->
                    context.oror(
                        irEqualsNull(irGet(valueSymbol.owner)),
                        irIs(irGet(valueSymbol.owner), type.makeNotNull())
                    )
                }
            }
            else -> irIs(argument, type.makeNotNull())
        }
    }

    private fun lowerCast(argument: IrExpression, type: IrType): IrExpression = when {
        type.isReifiedTypeParameter ->
            builder.irAs(argument, type)
        argument.type.isNullable() && !type.isNullable() ->
            with(builder) {
                irLetS(argument) { valueSymbol ->
                    irIfNull(
                        type,
                        irGet(valueSymbol.owner),
                        irThrow(irCall(typeCastException).apply {
                            putValueArgument(0, irString("null cannot be cast to non-null type ${type.render()}"))
                        }),
                        lowerCast(irGet(valueSymbol.owner), type.makeNullable())
                    )
                }
            }
        argument.type.isSubtypeOfClass(type.erasedUpperBound.symbol) ->
            argument
        else ->
            builder.irAs(argument, type)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression = with(builder) {
        at(expression)
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
                irComposite(resultType = expression.type) {
                    +expression.argument.transformVoid()
                    // TODO: Don't generate these casts in the first place
                    if (!expression.argument.type.isSubtypeOf(expression.type.makeNullable(), context.irBuiltIns)) {
                        +IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expression.type)
                    }
                }

            // There is no difference between IMPLICIT_CAST and IMPLICIT_INTEGER_COERCION on JVM_IR.
            // Instead, this is handled in StackValue.coerce.
            IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
                irImplicitCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.CAST ->
                lowerCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.SAFE_CAST ->
                if (expression.typeOperand.isReifiedTypeParameter) {
                    expression.transformChildrenVoid()
                    expression
                } else {
                    irLetS(expression.argument.transformVoid(), IrStatementOrigin.SAFE_CALL) { valueSymbol ->
                        irIfThenElse(
                            expression.type,
                            lowerInstanceOf(irGet(valueSymbol.owner), expression.typeOperand.makeNotNull()),
                            irGet(valueSymbol.owner),
                            irNull(expression.type)
                        )
                    }
                }

            IrTypeOperator.INSTANCEOF ->
                lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.NOT_INSTANCEOF ->
                irNot(lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand))

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val (startOffset, endOffset) = expression.extents()
                val source = sourceViewFor(parent as IrDeclaration).subSequence(startOffset, endOffset).toString()

                irLetS(expression.argument.transformVoid()) { valueSymbol ->
                    irComposite(resultType = expression.type) {
                        +irCall(checkExpressionValueIsNotNull).apply {
                            putValueArgument(0, irGet(valueSymbol.owner))
                            putValueArgument(1, irString(source))
                        }
                        +irGet(valueSymbol.owner)
                    }
                }
            }

            else -> {
                expression.transformChildrenVoid()
                expression
            }
        }
    }

    private fun IrElement.extents(): Pair<Int, Int> {
        var startOffset = UNDEFINED_OFFSET
        var endOffset = UNDEFINED_OFFSET
        acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
                if (startOffset == UNDEFINED_OFFSET || element.startOffset != UNDEFINED_OFFSET && element.startOffset < startOffset)
                    startOffset = element.startOffset
                if (endOffset == UNDEFINED_OFFSET || element.endOffset != UNDEFINED_OFFSET && endOffset < element.endOffset)
                    endOffset = element.endOffset
            }
        })
        return startOffset to endOffset
    }

    private fun sourceViewFor(declaration: IrDeclaration) =
        context.psiSourceManager.getKtFile(declaration.fileParent)!!.viewProvider.contents

    // Since Kotlin 1.4 we throw NullPointerExceptions instead of more specialized exception classes.
    private val useNullPointerExceptions: Boolean
        get() = context.state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4

    private val typeCastException: IrFunctionSymbol =
        if (useNullPointerExceptions)
            context.ir.symbols.ThrowNullPointerException
        else
            context.ir.symbols.ThrowTypeCastException

    private val checkExpressionValueIsNotNull: IrFunctionSymbol =
        if (useNullPointerExceptions)
            context.ir.symbols.checkNotNullExpressionValue
        else
            context.ir.symbols.checkExpressionValueIsNotNull
}
