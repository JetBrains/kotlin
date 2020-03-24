/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.ir.declarations.*

class Fir2IrConversionScope {
    private val parentStack = mutableListOf<IrDeclarationParent>()

    fun <T : IrDeclarationParent?> withParent(parent: T, f: T.() -> Unit): T {
        if (parent == null) return parent
        parentStack += parent
        parent.f()
        parentStack.removeAt(parentStack.size - 1)
        return parent
    }

    fun parentFromStack(): IrDeclarationParent = parentStack.last()

    fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T {
        declaration.parent = parentStack.last()
        return declaration
    }

    private val functionStack = mutableListOf<IrFunction>()

    fun <T : IrFunction> withFunction(function: T, f: T.() -> Unit): T {
        functionStack += function
        function.f()
        functionStack.removeAt(functionStack.size - 1)
        return function
    }

    private val propertyStack = mutableListOf<IrProperty>()

    fun withProperty(property: IrProperty, f: IrProperty.() -> Unit): IrProperty {
        propertyStack += property
        property.f()
        propertyStack.removeAt(propertyStack.size - 1)
        return property
    }

    private val classStack = mutableListOf<IrClass>()

    fun withClass(klass: IrClass, f: IrClass.() -> Unit): IrClass {
        classStack += klass
        klass.f()
        classStack.removeAt(classStack.size - 1)
        return klass
    }

    private val subjectVariableStack = mutableListOf<IrVariable>()

    fun <T> withSubject(subject: IrVariable?, f: () -> T): T {
        if (subject != null) subjectVariableStack += subject
        val result = f()
        if (subject != null) subjectVariableStack.removeAt(subjectVariableStack.size - 1)
        return result
    }

    fun returnTarget(expression: FirReturnExpression): IrFunction {
        val firTarget = expression.target.labeledElement
        for (potentialTarget in functionStack.asReversed()) {
            // TODO: remove comparison by name
            if (potentialTarget.name == (firTarget as? FirSimpleFunction)?.name) {
                return potentialTarget
            }
        }
        return functionStack.last()
    }

    fun parent(): IrDeclarationParent? = parentStack.lastOrNull()

    fun lastDispatchReceiverParameter(): IrValueParameter? {
        var dispatchReceiver = functionStack.lastOrNull()?.dispatchReceiverParameter
        // Use the dispatch receiver of the containing function
        dispatchReceiver?.let { return it }

        // Use the dispatch receiver of the enclosing function if any
        dispatchReceiver = functionStack.elementAtOrNull(functionStack.size - 2)?.dispatchReceiverParameter
        dispatchReceiver?.let { return it }

        // Use the dispatch receiver of the containing class
        val lastClassSymbol = classStack.lastOrNull()?.symbol
        return lastClassSymbol?.owner?.thisReceiver
    }

    fun lastClass(): IrClass? = classStack.lastOrNull()

    fun lastSubject(): IrVariable = subjectVariableStack.last()
}