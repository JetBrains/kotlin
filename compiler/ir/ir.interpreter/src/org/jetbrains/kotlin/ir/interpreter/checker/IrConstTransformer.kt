/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.isPrimitiveArray
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class IrConstTransformer(
    private val interpreter: IrInterpreter,
    private val irFile: IrFile,
    private val mode: EvaluationMode,
    private val onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    private val onError: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    private val suppressExceptions: Boolean = false,
) : IrElementTransformerVoid() {
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

    private fun IrExpression.canBeInterpreted(containingDeclaration: IrElement? = null): Boolean {
        return try {
            this.accept(IrCompileTimeChecker(containingDeclaration, mode = mode), null)
        } catch (e: Throwable) {
            if (suppressExceptions) {
                return false
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }
    }

    private fun IrExpression.interpret(failAsError: Boolean): IrExpression {
        val result = try {
            interpreter.interpret(this, irFile)
        } catch (e: Throwable) {
            if (suppressExceptions) {
                return this
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }

        return if (failAsError) result.reportIfError(this) else result.warningIfError(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.canBeInterpreted()) {
            return expression.interpret(failAsError = false)
        }
        return super.visitCall(expression)
    }

    override fun visitField(declaration: IrField): IrStatement {
        transformAnnotations(declaration)

        val initializer = declaration.initializer
        val expression = initializer?.expression ?: return declaration
        if (expression is IrConst<*>) return declaration
        val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
        if (isConst && expression.canBeInterpreted(declaration)) {
            initializer.expression = expression.interpret(failAsError = true)
        }

        return super.visitField(declaration)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        transformAnnotations(declaration)
        return super.visitDeclaration(declaration)
    }

    private fun transformAnnotations(annotationContainer: IrAnnotationContainer) {
        annotationContainer.annotations.forEach { annotation ->
            transformAnnotation(annotation)
        }
    }

    private fun transformAnnotation(annotation: IrConstructorCall) {
        for (i in 0 until annotation.valueArgumentsCount) {
            val arg = annotation.getValueArgument(i) ?: continue
            when (arg) {
                is IrVararg -> annotation.putValueArgument(i, arg.transformVarArg())
                else -> annotation.putValueArgument(i, arg.transformSingleArg(annotation.symbol.owner.valueParameters[i].type))
            }
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