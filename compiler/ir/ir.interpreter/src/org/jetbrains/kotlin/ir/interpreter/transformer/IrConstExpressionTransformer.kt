/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.createGetField
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import kotlin.math.max
import kotlin.math.min

internal abstract class IrConstExpressionTransformer(
    interpreter: IrInterpreter,
    irFile: IrFile,
    mode: EvaluationMode,
    checker: IrInterpreterChecker,
    evaluatedConstTracker: EvaluatedConstTracker?,
    inlineConstTracker: InlineConstTracker?,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    suppressExceptions: Boolean,
) : IrConstTransformer(
    interpreter, irFile, mode, checker, evaluatedConstTracker, inlineConstTracker, onWarning, onError, suppressExceptions
) {
    override fun visitFunction(declaration: IrFunction, data: Data): IrStatement {
        // It is useless to visit default accessor, and if we do that, we could render excess information for `IrGetField`
        if (declaration.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) return declaration

        // We want to be able to evaluate default arguments of annotation's constructor
        val isAnnotationConstructor = declaration is IrConstructor && declaration.parentClassOrNull?.kind == ClassKind.ANNOTATION_CLASS
        return super.visitFunction(declaration, data.copy(inConstantExpression = isAnnotationConstructor))
    }

    override fun visitCall(expression: IrCall, data: Data): IrElement {
        if (expression.canBeInterpreted()) {
            return expression.interpret(failAsError = data.inConstantExpression)
        }
        return super.visitCall(expression, data)
    }

    override fun visitField(declaration: IrField, data: Data): IrStatement {
        val initializer = declaration.initializer
        val expression = initializer?.expression ?: return declaration
        val getField = declaration.createGetField()

        if (getField.canBeInterpreted()) {
            initializer.expression = expression.interpret(failAsError = true)
        }

        return super.visitField(declaration, data)
    }

    override fun visitGetField(expression: IrGetField, data: Data): IrExpression {
        if (expression.canBeInterpreted()) {
            return expression.interpret(failAsError = data.inConstantExpression)
        }
        return super.visitGetField(expression, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Data): IrExpression {
        fun IrExpression.wrapInStringConcat(): IrExpression = IrStringConcatenationImpl(
            this.startOffset, this.endOffset, expression.type, listOf(this@wrapInStringConcat)
        )

        fun IrExpression.wrapInToStringConcatAndInterpret(): IrExpression = wrapInStringConcat().interpret(failAsError = data.inConstantExpression)
        fun IrExpression.getConstStringOrEmpty(): String = if (this is IrConst<*>) value.toString() else ""

        // If we have some complex expression in arguments (like some `IrComposite`) we will skip it,
        // but we must visit this argument in order to apply all possible optimizations.
        val transformed = super.visitStringConcatenation(expression, data) as? IrStringConcatenation ?: return expression
        // here `StringBuilder`'s list is used to optimize memory, everything works without it
        val folded = mutableListOf<IrExpression>()
        val buildersList = mutableListOf<StringBuilder>()
        for (next in transformed.arguments) {
            val last = folded.lastOrNull()
            when {
                !next.wrapInStringConcat().canBeInterpreted() -> {
                    folded += next
                    buildersList.add(StringBuilder(next.getConstStringOrEmpty()))
                }
                last == null || !last.wrapInStringConcat().canBeInterpreted() -> {
                    val result = next.wrapInToStringConcatAndInterpret()
                    folded += result
                    buildersList.add(StringBuilder(result.getConstStringOrEmpty()))
                }
                else -> {
                    val nextAsConst = next.wrapInToStringConcatAndInterpret()
                    if (nextAsConst !is IrConst<*>) {
                        folded += next
                        buildersList.add(StringBuilder(next.getConstStringOrEmpty()))
                    } else {
                        buildersList.last().append(nextAsConst.value.toString())
                        folded[folded.size - 1] = IrConstImpl.string(
                            // Inlined strings may have `last.startOffset > next.endOffset`
                            min(last.startOffset, next.startOffset), max(last.endOffset, next.endOffset),
                            expression.type, buildersList.last().toString()
                        )
                    }
                }
            }
        }

        val foldedConst = folded.singleOrNull() as? IrConst<*>
        if (foldedConst != null && foldedConst.value is String) {
            return IrConstImpl.string(expression.startOffset, expression.endOffset, expression.type, buildersList.single().toString())
        }

        return IrStringConcatenationImpl(expression.startOffset, expression.endOffset, expression.type, folded)
    }
}
