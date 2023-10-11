/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCheckerData
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCommonChecker
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterConstGetterPreprocessor
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterKCallableNamePreprocessor
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterPreprocessorData
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.interpreter.toConstantValue
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun IrElement.transformConst(
    irFile: IrFile,
    interpreter: IrInterpreter,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker? = null,
    inlineConstTracker: InlineConstTracker? = null,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    suppressExceptions: Boolean = false,
): IrElement {
    val checker = IrInterpreterCommonChecker()
    val irConstExpressionTransformer = IrConstOnlyNecessaryTransformer(
        interpreter, irFile, mode, checker, evaluatedConstTracker, inlineConstTracker, onWarning, onError, suppressExceptions
    )

    val irConstDeclarationAnnotationTransformer = IrConstDeclarationAnnotationTransformer(
        interpreter, irFile, mode, checker, evaluatedConstTracker, inlineConstTracker, onWarning, onError, suppressExceptions
    )

    val irConstTypeAnnotationTransformer = IrConstTypeAnnotationTransformer(
        interpreter, irFile, mode, checker, evaluatedConstTracker, inlineConstTracker, onWarning, onError, suppressExceptions
    )

    return this.transform(irConstExpressionTransformer, IrConstTransformer.Data())
        .transform(irConstDeclarationAnnotationTransformer, IrConstTransformer.Data())
        .transform(irConstTypeAnnotationTransformer, IrConstTransformer.Data())
}

fun IrFile.runConstOptimizations(
    interpreter: IrInterpreter,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker? = null,
    inlineConstTracker: InlineConstTracker? = null,
    suppressExceptions: Boolean = false,
) {
    val preprocessedFile = this.preprocessForConstTransformer(interpreter, mode)

    val checker = IrInterpreterCommonChecker()
    val irConstExpressionTransformer = IrConstAllTransformer(
        interpreter, preprocessedFile, mode, checker, evaluatedConstTracker, inlineConstTracker,
        { _, _, _ -> }, { _, _, _ -> },
        suppressExceptions
    )
    preprocessedFile.transform(irConstExpressionTransformer, IrConstTransformer.Data())
}

private fun IrFile.preprocessForConstTransformer(
    interpreter: IrInterpreter,
    mode: EvaluationMode,
): IrFile {
    val preprocessors = setOf(IrInterpreterKCallableNamePreprocessor(), IrInterpreterConstGetterPreprocessor())
    val preprocessedFile = preprocessors.fold(this) { file, preprocessor ->
        preprocessor.preprocess(file, IrInterpreterPreprocessorData(mode, interpreter.irBuiltIns))
    }
    return preprocessedFile
}

internal abstract class IrConstTransformer(
    private val interpreter: IrInterpreter,
    private val irFile: IrFile,
    private val mode: EvaluationMode,
    private val checker: IrInterpreterChecker,
    private val evaluatedConstTracker: EvaluatedConstTracker?,
    private val inlineConstTracker: InlineConstTracker?,
    private val onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    private val onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    private val suppressExceptions: Boolean,
) : IrElementTransformer<IrConstTransformer.Data> {
    internal data class Data(val inConstantExpression: Boolean = false)

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

    protected fun IrExpression.canBeInterpreted(): Boolean {
        return try {
            this.accept(checker, IrInterpreterCheckerData(irFile, mode, interpreter.irBuiltIns))
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

        if (result is IrConst<*>) {
            reportInlinedJavaConst(result)
        }

        return if (failAsError) result.reportIfError(this) else result.warningIfError(this)
    }

    private fun IrExpression.reportInlinedJavaConst(result: IrConst<*>) {
        this.acceptVoid(object : IrElementVisitorVoid {
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

fun InlineConstTracker.reportOnIr(irFile: IrFile, field: IrField, value: IrConst<*>) {
    if (field.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return

    val path = irFile.path
    val owner = field.parentAsClass.classId?.asString()?.replace(".", "$")?.replace("/", ".") ?: return
    val name = field.name.asString()
    val constType = value.kind.asString

    report(path, owner, name, constType)
}
