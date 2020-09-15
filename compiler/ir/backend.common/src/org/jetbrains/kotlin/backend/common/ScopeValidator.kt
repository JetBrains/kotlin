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
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/* Make sure that all the variable references and type parameter references are within the scope of the corresponding variables and
   type parameters.
*/
class ScopeValidator(
    private val reportError: ReportError
) : IrElementVisitorVoid {

    
    private val accessibleValues = mutableSetOf<IrValueDeclaration>()
    private val valuesStack = mutableListOf<MutableSet<IrValueDeclaration>>()

    private val accessibleTypeParameters = mutableSetOf<IrTypeParameter>()


    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        handleTypeParameterContainer(declaration.typeParameters) {
            for (superType in declaration.superTypes) {
                visitTypeAccess(declaration, superType)
            }
            handleValuesContainer(listOfNotNull(declaration.thisReceiver)) {
                super.visitClass(declaration)
            }
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        handleTypeParameterContainer(declaration.typeParameters) {
            visitTypeAccess(declaration, declaration.returnType)
            handleValuesContainer(declaration.valueParameters) {
                declaration.dispatchReceiverParameter?.let { addAccessibleValue(it) }
                declaration.extensionReceiverParameter?.let { addAccessibleValue(it) }
                super.visitFunction(declaration)
            }
        }
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        val primaryConstructor = declaration.parentAsClass.primaryConstructor()
        if (primaryConstructor == null) {
            super.visitAnonymousInitializer(declaration)
        } else {
            handleValuesContainer(primaryConstructor.valueParameters) {
                super.visitAnonymousInitializer(declaration)
            }
        }
    }

    override fun visitField(declaration: IrField) {
        visitTypeAccess(declaration, declaration.type)
        if (declaration.initializer == null) {
            return super.visitField(declaration)
        }
        val primaryConstructor = (declaration.parent as? IrClass)?.primaryConstructor()
        if (primaryConstructor == null) {
            super.visitField(declaration)
        } else {
            handleValuesContainer(primaryConstructor.valueParameters) {
                super.visitField(declaration)
            }
        }
    }

    override fun visitBlock(expression: IrBlock) {
        handleValuesContainer(emptyList()) {
            super.visitBlock(expression)
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        visitTypeAccess(declaration, declaration.type)
        super.visitVariable(declaration)
        addAccessibleValue(declaration)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        handleTypeParameterContainer(declaration.typeParameters) {
            visitTypeAccess(declaration, declaration.expandedType)
            super.visitTypeAlias(declaration)
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        for (superType in declaration.superTypes) {
            visitTypeAccess(declaration, superType)
        }
        super.visitTypeParameter(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        visitTypeAccess(declaration, declaration.type)
        declaration.varargElementType?.let { visitTypeAccess(declaration, it) }
        super.visitValueParameter(declaration)
    }

    override fun visitVariableAccess(expression: IrValueAccessExpression) {
        if (expression.symbol.owner !in accessibleValues) {
            reportError(expression, "Value ${expression.symbol.owner.render()} not accessible")
        }
        super.visitVariableAccess(expression)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
        for (i in 0 until expression.typeArgumentsCount) {
            expression.getTypeArgument(i)?.let { visitTypeAccess(expression, it) }
        }
        super.visitMemberAccess(expression)
    }

    override fun visitCatch(aCatch: IrCatch) {
        // catchParameter only has scope over result expression
        handleValuesContainer(emptyList()) {
            super.visitCatch(aCatch)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        visitTypeAccess(expression, expression.typeOperand)
        super.visitTypeOperator(expression)
    }

    private fun visitTypeAccess(element: IrElement, type: IrType) {
        if (type !is IrSimpleType) return
        if (type.classifier is IrTypeParameterSymbol && type.classifier.owner !in accessibleTypeParameters) {
            reportError(element, "Type parameter ${type.classifier.owner.render()} not accessible")
        }
        for (arg in type.arguments) {
            if (arg is IrTypeProjection) {
                visitTypeAccess(element, arg.type)
            }
        }
    }

    override fun visitExpression(expression: IrExpression) {
        visitTypeAccess(expression, expression.type)
        super.visitExpression(expression)
    }

    private inline fun handleTypeParameterContainer(typeParameters: List<IrTypeParameter>, block: () -> Unit) {
        accessibleTypeParameters.addAll(typeParameters)
        block()
        accessibleTypeParameters.removeAll(typeParameters)
    }

    private inline fun handleValuesContainer(values: List<IrValueDeclaration>, block: () -> Unit) {
        accessibleValues.addAll(values)
        valuesStack.add(values.toMutableSet())
        block()
        val toDrop = valuesStack.removeLast()
        accessibleValues.removeAll(toDrop)
    }

    private fun addAccessibleValue(value: IrValueDeclaration) {
        accessibleValues.add(value)
        valuesStack.last().add(value)
    }

    private fun IrClass.primaryConstructor(): IrConstructor? = constructors.singleOrNull { it.isPrimary }
}