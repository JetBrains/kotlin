/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCheckerData
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.interpreter.toConstantValue
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

internal class IrConstEvaluationContext(
    private val interpreter: IrInterpreter,
    private val irFile: IrFile,
    private val mode: EvaluationMode,
    private val checker: IrInterpreterChecker,
    private val evaluatedConstTracker: EvaluatedConstTracker?,
    private val inlineConstTracker: InlineConstTracker?,
    private val onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    private val onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    private val suppressExceptions: Boolean,
) {
    private var shouldSaveEvaluatedConstants = true

    private fun IrExpression.warningIfError(original: IrExpression): IrExpression {
        if (this is IrErrorExpression) {
            onWarning(irFile, original, this)
            return original
        }
        return this
    }

    private fun IrExpression.reportIfError(original: IrExpression): IrExpression {
        if (this is IrErrorExpression) {
            onError(irFile, original, this)
            return when (mode) {
                // need to pass any const value to be able to get some bytecode and then report error
                is EvaluationMode.OnlyIntrinsicConst -> IrConstImpl.Companion.constNull(startOffset, endOffset, type)
                else -> original
            }
        }
        return this
    }

    fun canBeInterpreted(expression: IrExpression): Boolean {
        return try {
            expression.accept(checker, IrInterpreterCheckerData(irFile, mode, interpreter.irBuiltIns))
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            if (suppressExceptions) {
                return false
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${expression.dump()}", e)
        }
    }

    fun interpret(expression: IrExpression, failAsError: Boolean): IrExpression {
        val result = try {
            interpreter.interpret(expression, irFile)
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            if (suppressExceptions) {
                return expression
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${expression.dump()}", e)
        }

        saveInConstTracker(result)

        if (result is IrConst) {
            reportInlinedJavaConst(expression, result)
        }

        return if (failAsError) result.reportIfError(expression) else result.warningIfError(expression)
    }

    fun saveInConstTracker(expression: IrExpression) {
        if (!shouldSaveEvaluatedConstants) return
        evaluatedConstTracker?.save(
            expression.startOffset, expression.endOffset, irFile.nameWithPackage,
            constant = if (expression is IrErrorExpression) ErrorValue.Companion.create(expression.description) else expression.toConstantValue()
        )
    }

    inline fun saveConstantsOnCondition(saveConstants: Boolean, block: () -> Unit) {
        val oldValue = shouldSaveEvaluatedConstants
        shouldSaveEvaluatedConstants = saveConstants
        try {
            block()
        } finally {
            shouldSaveEvaluatedConstants = oldValue
        }
    }

    private fun reportInlinedJavaConst(expression: IrExpression, result: IrConst) {
        expression.acceptVoid(object : IrLeafVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            private fun report(field: IrField) {
                inlineConstTracker?.reportOnIr(irFile, field, result)
            }

            override fun visitGetField(expression: IrGetField) {
                report(expression.symbol.owner)
                super.visitGetField(expression)
            }

            override fun visitCall(expression: IrCall) {
                expression.symbol.owner.property?.backingField?.let { backingField -> report(backingField) }
                super.visitCall(expression)
            }
        })
    }
}
