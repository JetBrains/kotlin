/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.isConst
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.JsStandardClassIds

/**
 * This transformer will visit all expressions and will evaluate only those that are necessary.
 * By "necessary" we mean expressions that are used in `const val`, inside annotations and js() call arguments.
 */
internal class IrConstOnlyNecessaryTransformer(
    interpreter: IrInterpreter,
    irFile: IrFile,
    mode: EvaluationMode,
    checker: IrInterpreterChecker,
    evaluatedConstTracker: EvaluatedConstTracker?,
    inlineConstTracker: InlineConstTracker?,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    suppressExceptions: Boolean,
) : IrConstExpressionTransformer(
    interpreter, irFile, mode, checker, evaluatedConstTracker, inlineConstTracker, onWarning, onError, suppressExceptions
) {
    private val jsCodeFqName = JsStandardClassIds.Callables.JsCode.asSingleFqName()

    override fun visitCall(expression: IrCall, data: Data): IrElement {
        val isJsCodeCall = expression.symbol.owner.fqNameWhenAvailable == jsCodeFqName
        if (isJsCodeCall) {
            // The `js` call itself can't be evaluated, we want ot evaluate its argument.
            expression.transformChildren(this, data.copy(inConstantExpression = true))
            return expression
        }

        val isConstGetter = expression.symbol.owner.property.isConst
        if (data.inConstantExpression || isConstGetter) {
            return super.visitCall(expression, data.copy(inConstantExpression = true))
        }
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitGetField(expression: IrGetField, data: Data): IrExpression {
        val isConst = expression.symbol.owner.property.isConst
        if (data.inConstantExpression || isConst) {
            return super.visitGetField(expression, data.copy(inConstantExpression = true))
        }
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Data): IrExpression {
        if (data.inConstantExpression) {
            return super.visitStringConcatenation(expression, data.copy(inConstantExpression = true))
        }
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitField(declaration: IrField, data: Data): IrStatement {
        val isConst = declaration.property.isConst
        if (isConst) {
            return super.visitField(declaration, data.copy(inConstantExpression = true))
        }
        declaration.transformChildren(this, data)
        return declaration
    }
}
