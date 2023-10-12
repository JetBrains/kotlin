/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.visitors

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.declarations.BirLocalDelegatedProperty
import org.jetbrains.kotlin.bir.declarations.BirScript
import org.jetbrains.kotlin.bir.declarations.BirTypeAlias
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirClassReference
import org.jetbrains.kotlin.bir.expressions.BirConstantObject
import org.jetbrains.kotlin.bir.expressions.BirConstantValue
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirTypeOperatorCall
import org.jetbrains.kotlin.bir.expressions.BirVararg
import org.jetbrains.kotlin.bir.types.BirType

interface BirTypeTransformer<in D> : BirElementTransformer<D> {
    fun <Type : BirType?> transformType(
        container: BirElement,
        type: Type,
        data: D,
    ): Type

    override fun visitValueParameter(declaration: BirValueParameter, data: D):
            BirStatement {
        declaration.varargElementType = transformType(declaration, declaration.varargElementType,
                data)
        declaration.type = transformType(declaration, declaration.type, data)
        return super.visitValueParameter(declaration, data)
    }

    override fun visitClass(declaration: BirClass, data: D): BirStatement {
        declaration.valueClassRepresentation?.mapUnderlyingType {
            transformType(declaration, it, data)
        }
        declaration.superTypes = declaration.superTypes.map { transformType(declaration, it, data) }
        return super.visitClass(declaration, data)
    }

    override fun visitTypeParameter(declaration: BirTypeParameter, data: D): BirStatement {
        declaration.superTypes = declaration.superTypes.map { transformType(declaration, it, data) }
        return super.visitTypeParameter(declaration, data)
    }

    override fun visitFunction(declaration: BirFunction, data: D): BirStatement {
        declaration.returnType = transformType(declaration, declaration.returnType, data)
        return super.visitFunction(declaration, data)
    }

    override fun visitField(declaration: BirField, data: D): BirStatement {
        declaration.type = transformType(declaration, declaration.type, data)
        return super.visitField(declaration, data)
    }

    override fun visitLocalDelegatedProperty(declaration: BirLocalDelegatedProperty,
            data: D): BirStatement {
        declaration.type = transformType(declaration, declaration.type, data)
        return super.visitLocalDelegatedProperty(declaration, data)
    }

    override fun visitScript(declaration: BirScript, data: D): BirStatement {
        declaration.baseClass = transformType(declaration, declaration.baseClass, data)
        return super.visitScript(declaration, data)
    }

    override fun visitTypeAlias(declaration: BirTypeAlias, data: D): BirStatement {
        declaration.expandedType = transformType(declaration, declaration.expandedType, data)
        return super.visitTypeAlias(declaration, data)
    }

    override fun visitVariable(declaration: BirVariable, data: D): BirStatement {
        declaration.type = transformType(declaration, declaration.type, data)
        return super.visitVariable(declaration, data)
    }

    override fun visitExpression(expression: BirExpression, data: D): BirExpression {
        expression.type = transformType(expression, expression.type, data)
        return super.visitExpression(expression, data)
    }

    override fun visitMemberAccess(expression: BirMemberAccessExpression<*>, data: D):
            BirElement {
        (0 until expression.typeArgumentsCount).forEach {
            expression.getTypeArgument(it)?.let { type ->
                expression.putTypeArgument(it, transformType(expression, type, data))
            }
        }
        return super.visitMemberAccess(expression, data)
    }

    override fun visitClassReference(expression: BirClassReference, data: D):
            BirExpression {
        expression.classType = transformType(expression, expression.classType, data)
        return super.visitClassReference(expression, data)
    }

    override fun visitConstantObject(expression: BirConstantObject, data: D):
            BirConstantValue {
        for (i in 0 until expression.typeArguments.size) {
            expression.typeArguments[i] = transformType(expression, expression.typeArguments[i],
                    data)
        }
        return super.visitConstantObject(expression, data)
    }

    override fun visitTypeOperator(expression: BirTypeOperatorCall, data: D):
            BirExpression {
        expression.typeOperand = transformType(expression, expression.typeOperand, data)
        return super.visitTypeOperator(expression, data)
    }

    override fun visitVararg(expression: BirVararg, data: D): BirExpression {
        expression.varargElementType = transformType(expression, expression.varargElementType, data)
        return super.visitVararg(expression, data)
    }
}
