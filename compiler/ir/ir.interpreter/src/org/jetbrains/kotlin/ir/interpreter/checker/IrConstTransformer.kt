/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.toConstantValue
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

fun IrFile.transformConst(
    interpreter: IrInterpreter,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker? = null,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    suppressExceptions: Boolean = false,
) {
    val irConstExpressionTransformer = IrConstExpressionTransformer(
        interpreter, this, mode, evaluatedConstTracker, onWarning, onError, suppressExceptions
    )
    val irConstDeclarationAnnotationTransformer = IrConstDeclarationAnnotationTransformer(
        interpreter, this, mode, evaluatedConstTracker, onWarning, onError, suppressExceptions
    )
    val irConstTypeAnnotationTransformer = IrConstTypeAnnotationTransformer(
        interpreter, this, mode, evaluatedConstTracker, onWarning, onError, suppressExceptions
    )
    this.transform(irConstExpressionTransformer, null)
    this.transform(irConstDeclarationAnnotationTransformer, null)
    this.transform(irConstTypeAnnotationTransformer, null)
}

// Note: We are using `IrElementTransformer` here instead of `IrElementTransformerVoid` to avoid conflicts with `IrTypeVisitorVoid`
// that is used later in `IrConstTypeAnnotationTransformer`.
internal abstract class IrConstTransformer(
    protected val interpreter: IrInterpreter,
    private val irFile: IrFile,
    private val mode: EvaluationMode,
    private val evaluatedConstTracker: EvaluatedConstTracker? = null,
    private val onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    private val onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    private val suppressExceptions: Boolean,
) : IrElementTransformer<Nothing?> {
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
                EvaluationMode.ONLY_INTRINSIC_CONST -> IrConstImpl.constNull(startOffset, endOffset, type)
                else -> original
            }
        }
        return this
    }

    protected fun IrExpression.canBeInterpreted(
        containingDeclaration: IrElement? = null,
        configuration: IrInterpreterConfiguration = interpreter.environment.configuration
    ): Boolean {
        return try {
            this.accept(IrCompileTimeChecker(containingDeclaration, mode, configuration), null)
        } catch (e: Throwable) {
            if (suppressExceptions) {
                return false
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }
    }

    protected fun IrExpression.interpret(failAsError: Boolean): IrExpression {
        val result = try {
            interpreter.interpret(this, irFile)
        } catch (e: Throwable) {
            if (suppressExceptions) {
                return this
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }

        evaluatedConstTracker?.save(
            result.startOffset, result.endOffset, irFile.nameWithPackage,
            constant = if (result is IrErrorExpression) ErrorValue.create(result.description)
            else (result as IrConst<*>).toConstantValue()
        )
        return if (failAsError) result.reportIfError(this) else result.warningIfError(this)
    }
}
