/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.parentClassOrNull

class Fir2IrConversionScope {
    private val parentStack = mutableListOf<IrDeclarationParent>()

    private val containingFirClassStack = mutableListOf<FirClass<*>>()

    fun <T : IrDeclarationParent?> withParent(parent: T, f: T.() -> Unit): T {
        if (parent == null) return parent
        parentStack += parent
        parent.f()
        parentStack.removeAt(parentStack.size - 1)
        return parent
    }

    fun containingFileIfAny(): IrFile? = parentStack.getOrNull(0) as? IrFile

    fun withContainingFirClass(containingFirClass: FirClass<*>, f: () -> Unit) {
        containingFirClassStack += containingFirClass
        f()
        containingFirClassStack.removeAt(containingFirClassStack.size - 1)
    }

    fun parentFromStack(): IrDeclarationParent = parentStack.last()

    fun parentAccessorOfPropertyFromStack(property: IrProperty): IrSimpleFunction? {
        for (parent in parentStack.asReversed()) {
            when (parent) {
                property.getter -> return property.getter
                property.setter -> return property.setter
            }
        }
        return null
    }

    fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T {
        declaration.parent = parentStack.last()
        return declaration
    }

    fun containerFirClass(): FirClass<*>? = containingFirClassStack.lastOrNull()

    private val functionStack = mutableListOf<IrFunction>()

    fun <T : IrFunction> withFunction(function: T, f: T.() -> Unit): T {
        functionStack += function
        function.f()
        functionStack.removeAt(functionStack.size - 1)
        return function
    }

    private val propertyStack = mutableListOf<Pair<IrProperty, FirProperty?>>()

    fun withProperty(property: IrProperty, firProperty: FirProperty? = null, f: IrProperty.() -> Unit): IrProperty {
        propertyStack += (property to firProperty)
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

    private val whenSubjectVariableStack = mutableListOf<IrVariable>()
    private val safeCallSubjectVariableStack = mutableListOf<IrVariable>()

    fun <T> withWhenSubject(subject: IrVariable?, f: () -> T): T {
        if (subject != null) whenSubjectVariableStack += subject
        val result = f()
        if (subject != null) whenSubjectVariableStack.removeAt(whenSubjectVariableStack.size - 1)
        return result
    }

    fun <T> withSafeCallSubject(subject: IrVariable?, f: () -> T): T {
        if (subject != null) safeCallSubjectVariableStack += subject
        val result = f()
        if (subject != null) safeCallSubjectVariableStack.removeAt(safeCallSubjectVariableStack.size - 1)
        return result
    }

    fun returnTarget(expression: FirReturnExpression, declarationStorage: Fir2IrDeclarationStorage): IrFunction {
        val firTarget = expression.target.labeledElement
        val irTarget = (firTarget as? FirFunction)?.let {
            when (it) {
                is FirConstructor -> declarationStorage.getCachedIrConstructor(it)
                is FirPropertyAccessor -> {
                    for ((property, firProperty) in propertyStack.asReversed()) {
                        if (firProperty?.getter === firTarget) {
                            return@let property.getter
                        } else if (firProperty?.setter === firTarget) {
                            return@let property.setter
                        }
                    }
                }
                else -> declarationStorage.getCachedIrFunction(it)
            }
        }
        for (potentialTarget in functionStack.asReversed()) {
            if (potentialTarget == irTarget) {
                return potentialTarget
            }
        }
        return functionStack.last()
    }

    fun parent(): IrDeclarationParent? = parentStack.lastOrNull()

    fun dispatchReceiverParameter(irClass: IrClass): IrValueParameter? {
        for (function in functionStack.asReversed()) {
            if (function.parentClassOrNull == irClass) {
                // An inner class's constructor needs an instance of the outer class as a dispatch receiver.
                // However, if we are converting `this` receiver inside that constructor, now we should point to the inner class instance.
                if (function is IrConstructor && irClass.isInner) {
                    irClass.thisReceiver?.let { return it }
                }
                function.dispatchReceiverParameter?.let { return it }
            }
        }
        return irClass.thisReceiver
    }

    fun lastClass(): IrClass? = classStack.lastOrNull()

    fun lastWhenSubject(): IrVariable = whenSubjectVariableStack.last()
    fun lastSafeCallSubject(): IrVariable = safeCallSubjectVariableStack.last()
}
