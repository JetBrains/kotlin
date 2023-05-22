/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.isPrimitiveArray
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.types.*

internal abstract class IrConstAnnotationTransformer(
    interpreter: IrInterpreter,
    irFile: IrFile,
    mode: EvaluationMode,
    checker: IrInterpreterChecker,
    evaluatedConstTracker: EvaluatedConstTracker?,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    suppressExceptions: Boolean,
) : IrConstTransformer(interpreter, irFile, mode, checker, evaluatedConstTracker, onWarning, onError, suppressExceptions) {
    protected fun transformAnnotations(annotationContainer: IrAnnotationContainer) {
        annotationContainer.annotations.forEach { annotation ->
            transformAnnotation(annotation)
        }
    }

    private fun transformAnnotation(annotation: IrConstructorCall) {
        for (i in 0 until annotation.valueArgumentsCount) {
            val arg = annotation.getValueArgument(i) ?: continue
            annotation.putValueArgument(i, transformAnnotationArgument(arg, annotation.symbol.owner.valueParameters[i]))
        }
    }

    protected fun transformAnnotationArgument(argument: IrExpression, valueParameter: IrValueParameter): IrExpression {
        return when (argument) {
            is IrVararg -> argument.transformVarArg()
            else -> argument.transformSingleArg(valueParameter.type)
        }
    }

    private fun IrVararg.transformVarArg(): IrVararg {
        if (elements.isEmpty()) return this
        val newIrVararg = IrVarargImpl(this.startOffset, this.endOffset, this.type, this.varargElementType)
        for (element in this.elements) {
            when (val arg = (element as? IrSpreadElement)?.expression ?: element) {
                is IrVararg -> arg.transformVarArg().elements.forEach { newIrVararg.addElement(it) }
                is IrExpression -> newIrVararg.addElement(arg.transformSingleArg(this.varargElementType))
                else -> newIrVararg.addElement(arg)
            }
        }
        return newIrVararg
    }

    private fun IrExpression.transformSingleArg(expectedType: IrType): IrExpression {
        if (this.canBeInterpreted()) {
            return this.interpret(failAsError = true).convertToConstIfPossible(expectedType)
        } else if (this is IrConstructorCall) {
            transformAnnotation(this)
        }
        return this
    }

    private fun IrExpression.convertToConstIfPossible(type: IrType): IrExpression {
        return when {
            this !is IrConst<*> || type is IrErrorType -> this
            type.isArray() -> this.convertToConstIfPossible((type as IrSimpleType).arguments.single().typeOrNull!!)
            type.isPrimitiveArray() -> this.convertToConstIfPossible(this.type)
            else -> this.value.toIrConst(type, this.startOffset, this.endOffset)
        }
    }
}
