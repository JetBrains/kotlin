/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.parentClassOrNull

class Fir2IrConversionScope {
    private val parentStack = mutableListOf<IrDeclarationParent>()

    private val containingFirClassStack = mutableListOf<FirClass>()
    private val currentlyGeneratedDelegatedConstructors = mutableMapOf<IrClass, IrConstructor>()

    fun <T : IrDeclarationParent, R> withParent(parent: T, f: T.() -> R): R {
        parentStack += parent
        try {
            return parent.f()
        } finally {
            parentStack.removeAt(parentStack.size - 1)
        }
    }

    fun <T> forDelegatingConstructorCall(constructor: IrConstructor, irClass: IrClass, f: () -> T): T {
        currentlyGeneratedDelegatedConstructors[irClass] = constructor
        val result = f()
        currentlyGeneratedDelegatedConstructors.remove(irClass)
        return result
    }

    fun getConstructorForCurrentlyGeneratedDelegatedConstructor(itClass: IrClass): IrConstructor? =
        currentlyGeneratedDelegatedConstructors[itClass]

    fun containingFileIfAny(): IrFile? = parentStack.getOrNull(0) as? IrFile

    fun withContainingFirClass(containingFirClass: FirClass, f: () -> Unit) {
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

    fun containerFirClass(): FirClass? = containingFirClassStack.lastOrNull()

    private val functionStack = mutableListOf<IrFunction>()

    fun <T : IrFunction, R> withFunction(function: T, f: T.() -> R): R {
        functionStack += function
        try {
            return function.f()
        } finally {
            functionStack.removeAt(functionStack.size - 1)
        }
    }

    private val propertyStack = mutableListOf<Pair<IrProperty, FirProperty?>>()

    fun <R> withProperty(property: IrProperty, firProperty: FirProperty? = null, f: IrProperty.() -> R): R {
        propertyStack += (property to firProperty)
        try {
            return property.f()
        } finally {
            propertyStack.removeAt(propertyStack.size - 1)
        }
    }

    private val classStack = mutableListOf<IrClass>()

    fun <R> withClass(klass: IrClass, f: IrClass.() -> R): R {
        classStack += klass
        return try {
            klass.f()
        } finally {
            classStack.removeAt(classStack.size - 1)
        }
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
        val irTarget = when (val firTarget = expression.target.labeledElement) {
            is FirConstructor -> declarationStorage.getCachedIrConstructor(firTarget)
            is FirPropertyAccessor -> {
                var answer: IrFunction? = null
                for ((property, firProperty) in propertyStack.asReversed()) {
                    if (firProperty?.getter === firTarget) {
                        answer = property.getter
                    } else if (firProperty?.setter === firTarget) {
                        answer = property.setter
                    }
                }
                answer
            }
            else -> declarationStorage.getCachedIrFunction(firTarget)
        }
        for (potentialTarget in functionStack.asReversed()) {
            if (potentialTarget == irTarget) {
                return potentialTarget
            }
        }
        return functionStack.last()
    }

    fun parent(): IrDeclarationParent? = parentStack.lastOrNull()

    fun defaultConversionTypeOrigin(): ConversionTypeOrigin =
        if ((parent() as? IrFunction)?.isSetter == true) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT

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
