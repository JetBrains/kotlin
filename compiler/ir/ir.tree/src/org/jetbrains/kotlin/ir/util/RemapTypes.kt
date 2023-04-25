/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.memoryOptimizedMap

// Modify IrElement in place, applying typeRemapper to all the IrType fields.
fun IrElement.remapTypes(typeRemapper: TypeRemapper) {
    acceptVoid(RemapTypesHelper(typeRemapper))
}

private class RemapTypesHelper(private val typeRemapper: TypeRemapper) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.superTypes = declaration.superTypes.memoryOptimizedMap { typeRemapper.remapType(it) }
        super.visitClass(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        declaration.type = typeRemapper.remapType(declaration.type)
        declaration.varargElementType = declaration.varargElementType?.let { typeRemapper.remapType(it) }
        super.visitValueParameter(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        declaration.superTypes = declaration.superTypes.memoryOptimizedMap { typeRemapper.remapType(it) }
        super.visitTypeParameter(declaration)
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.type = typeRemapper.remapType(declaration.type)
        super.visitVariable(declaration)
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.returnType = typeRemapper.remapType(declaration.returnType)
        super.visitFunction(declaration)
    }

    override fun visitField(declaration: IrField) {
        declaration.type = typeRemapper.remapType(declaration.type)
        super.visitField(declaration)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        declaration.type = typeRemapper.remapType(declaration.type)
        super.visitLocalDelegatedProperty(declaration)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        declaration.expandedType = typeRemapper.remapType(declaration.expandedType)
        super.visitTypeAlias(declaration)
    }

    override fun visitExpression(expression: IrExpression) {
        expression.type = typeRemapper.remapType(expression.type)
        super.visitExpression(expression)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
        for (i in 0 until expression.typeArgumentsCount) {
            expression.getTypeArgument(i)?.let { expression.putTypeArgument(i, typeRemapper.remapType(it)) }
        }
        super.visitMemberAccess(expression)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        expression.typeOperand = typeRemapper.remapType(expression.typeOperand)
        super.visitTypeOperator(expression)
    }

    override fun visitVararg(expression: IrVararg) {
        expression.varargElementType = typeRemapper.remapType(expression.varargElementType)
        super.visitVararg(expression)
    }

    override fun visitClassReference(expression: IrClassReference) {
        expression.classType = typeRemapper.remapType(expression.classType)
        super.visitClassReference(expression)
    }
}