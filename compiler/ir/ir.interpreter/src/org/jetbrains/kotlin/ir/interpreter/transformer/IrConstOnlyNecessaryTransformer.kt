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

/**
 * This transformer will visit all expressions and will evaluate only those that are necessary. By "necessary" we mean expressions
 * that are used in `const val` and inside annotations.
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
    override fun visitCall(expression: IrCall, data: Data): IrElement {
        val isConstGetter = (expression.symbol.owner as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.isConst == true
        if (!data.inAnnotation && !isConstGetter) {
            expression.transformChildren(this, data)
            return expression
        }
        return super.visitCall(expression, data)
    }

    override fun visitGetField(expression: IrGetField, data: Data): IrExpression {
        val isConst = expression.symbol.owner.correspondingPropertySymbol?.owner?.isConst == true
        if (!data.inAnnotation && !isConst) return expression
        return super.visitGetField(expression, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Data): IrExpression {
        if (!data.inAnnotation) {
            expression.transformChildren(this, data)
            return expression
        }
        return super.visitStringConcatenation(expression, data)
    }

    override fun visitField(declaration: IrField, data: Data): IrStatement {
        val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
        if (!isConst) {
            declaration.transformChildren(this, data)
            return declaration
        }

        return super.visitField(declaration, data)
    }
}
