/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.interpreter.isPrimitiveArray
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.toIrConst

internal abstract class IrConstAnnotationTransformer(private val context: IrConstEvaluationContext) {

    abstract fun visitAnnotations(element: IrElement)

    protected fun transformAnnotations(annotationContainer: IrAnnotationContainer) {
        val declarationIsFakeOverride = (annotationContainer as? IrDeclaration)?.origin == IrDeclarationOrigin.FAKE_OVERRIDE
        annotationContainer.annotations.forEach { annotation ->
            context.saveConstantsOnCondition(!declarationIsFakeOverride) {
                transformAnnotation(annotation)
            }
        }
    }

    private fun transformAnnotation(annotation: IrConstructorCall) {
        if (annotation.type is IrErrorType) return
        for (i in 0 until annotation.valueArgumentsCount) {
            val arg = annotation.getValueArgument(i) ?: continue
            annotation.putValueArgument(i, transformAnnotationArgument(arg, annotation.symbol.owner.valueParameters[i]))
        }
        context.saveInConstTracker(annotation)
    }

    private fun transformAnnotationArgument(argument: IrExpression, valueParameter: IrValueParameter): IrExpression? {
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
                is IrExpression -> arg.transformSingleArg(this.varargElementType)?.let(newIrVararg::addElement)
                else -> newIrVararg.addElement(arg)
            }
        }
        return newIrVararg
    }

    private fun IrExpression.transformSingleArg(expectedType: IrType): IrExpression? {
        return when {
            this is IrGetClass && argument.type is IrErrorType -> null
            this is IrGetEnumValue || this is IrClassReference -> this
            this is IrConstructorCall && this.type.isAnnotation() -> {
                transformAnnotation(this)
                this
            }
            context.canBeInterpreted(this) -> context
                .interpret(this, failAsError = true)
                .convertToConstIfPossible(expectedType)
            else -> error("Cannot evaluate IR expression in annotation:\n ${this.dump()}")
        }
    }

    private fun IrExpression.convertToConstIfPossible(type: IrType): IrExpression {
        return when {
            this !is IrConst || type is IrErrorType -> this
            type.isArray() -> this.convertToConstIfPossible((type as IrSimpleType).arguments.single().typeOrNull!!)
            type.isPrimitiveArray() -> this.convertToConstIfPossible(this.type)
            else -> this.value.toIrConst(type, this.startOffset, this.endOffset)
        }
    }
}
