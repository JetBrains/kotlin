/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType

interface IrTypeTransformerVoid<in D> : IrElementTransformer<D> {
    fun <Type : IrType?> transformType(
        container: IrElement,
        type: Type,
        data: D,
    ): Type

    override fun visitValueParameter(declaration: IrValueParameter, data: D) = run {
        declaration.varargElementType = transformType(declaration, declaration.varargElementType,
                data)
        declaration.type = transformType(declaration, declaration.type, data)
        return@run super.visitValueParameter(declaration, data)
    }

    override fun visitClass(declaration: IrClass, data: D) = run {
        declaration.valueClassRepresentation?.mapUnderlyingType {
            transformType(declaration, it, data)
        }
        declaration.superTypes = declaration.superTypes.map { transformType(declaration, it, data) }
        return@run super.visitClass(declaration, data)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: D) = run {
        declaration.superTypes = declaration.superTypes.map { transformType(declaration, it, data) }
        return@run super.visitTypeParameter(declaration, data)
    }

    override fun visitFunction(declaration: IrFunction, data: D) = run {
        declaration.returnType = transformType(declaration, declaration.returnType, data)
        return@run super.visitFunction(declaration, data)
    }

    override fun visitField(declaration: IrField, data: D) = run {
        declaration.type = transformType(declaration, declaration.type, data)
        return@run super.visitField(declaration, data)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty,
            data: D) = run {
        declaration.type = transformType(declaration, declaration.type, data)
        return@run super.visitLocalDelegatedProperty(declaration, data)
    }

    override fun visitScript(declaration: IrScript, data: D) = run {
        declaration.baseClass = transformType(declaration, declaration.baseClass, data)
        return@run super.visitScript(declaration, data)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: D) = run {
        declaration.expandedType = transformType(declaration, declaration.expandedType, data)
        return@run super.visitTypeAlias(declaration, data)
    }

    override fun visitVariable(declaration: IrVariable, data: D) = run {
        declaration.type = transformType(declaration, declaration.type, data)
        return@run super.visitVariable(declaration, data)
    }

    override fun visitExpression(expression: IrExpression, data: D) = run {
        expression.type = transformType(expression, expression.type, data)
        return@run super.visitExpression(expression, data)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: D) =
            run {
        (0 until expression.typeArgumentsCount).forEach {
            expression.getTypeArgument(it)?.let { type ->
                expression.putTypeArgument(it, transformType(expression, type, data))
            }
        }
        return@run super.visitMemberAccess(expression, data)
    }

    override fun visitClassReference(expression: IrClassReference, data: D) = run {
        expression.classType = transformType(expression, expression.classType, data)
        return@run super.visitClassReference(expression, data)
    }

    override fun visitConstantObject(expression: IrConstantObject, data: D) = run {
        for (i in 0 until expression.typeArguments.size) {
            expression.typeArguments[i] = transformType(expression, expression.typeArguments[i],
                    data)
        }
        return@run super.visitConstantObject(expression, data)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: D) = run {
        expression.typeOperand = transformType(expression, expression.typeOperand, data)
        return@run super.visitTypeOperator(expression, data)
    }

    override fun visitVararg(expression: IrVararg, data: D) = run {
        expression.varargElementType = transformType(expression, expression.varargElementType, data)
        return@run super.visitVararg(expression, data)
    }
}
