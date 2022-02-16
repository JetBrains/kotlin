/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/* Make sure that all the variable references and type parameter references are within the scope of the corresponding variables and
   type parameters.
*/
class ScopeValidator(
    private val reportError: ReportError
) {

    fun check(element: IrElement) {
        element.accept(Checker(), Visibles(emptySet(), mutableSetOf()))
    }

    inner class Visibles(val typeParameters: Set<IrTypeParameter>, val values: MutableSet<IrValueDeclaration>) {
        fun visitTypeAccess(element: IrElement, type: IrType) {
            if (type !is IrSimpleType) return
            if (type.classifier is IrTypeParameterSymbol && type.classifier.owner !in this.typeParameters) {
                reportError(element, "Type parameter ${type.classifier.owner.render()} not accessible")
            }
            for (arg in type.arguments) {
                if (arg is IrTypeProjection) {
                    visitTypeAccess(element, arg.type)
                }
            }
        }

        fun visitValueAccess(element: IrElement, variable: IrValueDeclaration) {
            if (variable !in this.values) {
                reportError(element, "Value ${variable.render()} not accessible")
            }
        }

        fun extend(newTypeParameters: Collection<IrTypeParameter>, newValues: Collection<IrValueDeclaration>): Visibles =
            Visibles(
                if (newTypeParameters.isEmpty()) typeParameters else typeParameters + newTypeParameters,
                (values + newValues).toMutableSet()
            )
    }

    inner class Checker : IrElementVisitor<Unit, Visibles> {
        override fun visitElement(element: IrElement, data: Visibles) {
            element.acceptChildren(this, data)
        }

        override fun visitClass(declaration: IrClass, data: Visibles) {
            val newVisibles = data.extend(declaration.typeParameters, listOfNotNull(declaration.thisReceiver))
            for (superType in declaration.superTypes) {
                newVisibles.visitTypeAccess(declaration, superType)
            }
            super.visitClass(declaration, newVisibles)
        }

        override fun visitFunction(declaration: IrFunction, data: Visibles) {
            val newVisibles = data.extend(
                declaration.typeParameters,
                listOfNotNull(declaration.dispatchReceiverParameter, declaration.extensionReceiverParameter) + declaration.valueParameters
            )

            newVisibles.visitTypeAccess(declaration, declaration.returnType)
            super.visitFunction(declaration, newVisibles)
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Visibles) {
            val primaryConstructor = declaration.parentAsClass.primaryConstructor()
            if (primaryConstructor == null) {
                super.visitAnonymousInitializer(declaration, data)
            } else {
                super.visitAnonymousInitializer(declaration, data.extend(emptySet(), primaryConstructor.valueParameters))
            }
        }

        override fun visitField(declaration: IrField, data: Visibles) {
            data.visitTypeAccess(declaration, declaration.type)
            if (declaration.initializer == null) {
                return super.visitField(declaration, data)
            }
            val primaryConstructor = (declaration.parent as? IrClass)?.primaryConstructor()
            if (primaryConstructor == null) {
                super.visitField(declaration, data)
            } else {
                super.visitField(declaration, data.extend(emptySet(), primaryConstructor.valueParameters))
            }
        }

        override fun visitBlock(expression: IrBlock, data: Visibles) {
            // Entering a new scope
            super.visitBlock(expression, data.extend(emptySet(), emptySet()))
        }

        override fun visitVariable(declaration: IrVariable, data: Visibles) {
            data.visitTypeAccess(declaration, declaration.type)
            super.visitVariable(declaration, data)
            data.values.add(declaration)
        }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: Visibles) {
            val newVisibles = data.extend(declaration.typeParameters, emptySet())
            newVisibles.visitTypeAccess(declaration, declaration.expandedType)
            super.visitTypeAlias(declaration, newVisibles)
        }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: Visibles) {
            for (superType in declaration.superTypes) {
                data.visitTypeAccess(declaration, superType)
            }
            super.visitTypeParameter(declaration, data)
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: Visibles) {
            data.visitTypeAccess(declaration, declaration.type)
            declaration.varargElementType?.let { data.visitTypeAccess(declaration, it) }
            super.visitValueParameter(declaration, data)
        }

        override fun visitValueAccess(expression: IrValueAccessExpression, data: Visibles) {
            data.visitValueAccess(expression, expression.symbol.owner)
            super.visitValueAccess(expression, data)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: Visibles) {
            for (i in 0 until expression.typeArgumentsCount) {
                expression.getTypeArgument(i)?.let { data.visitTypeAccess(expression, it) }
            }
            super.visitMemberAccess(expression, data)
        }

        override fun visitCatch(aCatch: IrCatch, data: Visibles) {
            // catchParameter only has scope over result expression, so create a new scope
            super.visitCatch(aCatch, data.extend(emptySet(), emptySet()))
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Visibles) {
            data.visitTypeAccess(expression, expression.typeOperand)
            super.visitTypeOperator(expression, data)
        }

        override fun visitClassReference(expression: IrClassReference, data: Visibles) {
            // classType should only contain star projections, but check it to be sure.
            data.visitTypeAccess(expression, expression.classType)
            super.visitClassReference(expression, data)
        }

        override fun visitVararg(expression: IrVararg, data: Visibles) {
            data.visitTypeAccess(expression, expression.varargElementType)
            super.visitVararg(expression, data)
        }

        override fun visitExpression(expression: IrExpression, data: Visibles) {
            data.visitTypeAccess(expression, expression.type)
            super.visitExpression(expression, data)
        }

        private fun IrClass.primaryConstructor(): IrConstructor? = constructors.singleOrNull { it.isPrimary }
    }
}