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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbolInternals
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.PrivateForInline

@OptIn(PrivateForInline::class)
class Fir2IrConversionScope(val configuration: Fir2IrConfiguration) {
    @PublishedApi
    @PrivateForInline
    internal val parentStack = mutableListOf<IrDeclarationParent>()

    @PublishedApi
    @PrivateForInline
    internal val containingFirClassStack = mutableListOf<FirClass>()

    @PublishedApi
    @PrivateForInline
    internal val currentlyGeneratedDelegatedConstructors = mutableMapOf<IrClassSymbol, IrConstructor>()

    inline fun <T : IrDeclarationParent, R> withParent(parent: T, f: T.() -> R): R {
        parentStack += parent
        try {
            return parent.f()
        } finally {
            parentStack.removeAt(parentStack.size - 1)
        }
    }

    internal fun <T> forDelegatingConstructorCall(constructor: IrConstructor, irClass: IrClass, f: () -> T): T {
        currentlyGeneratedDelegatedConstructors[irClass.symbol] = constructor
        try {
            return f()
        } finally {
            currentlyGeneratedDelegatedConstructors.remove(irClass.symbol)
        }
    }

    fun getConstructorForCurrentlyGeneratedDelegatedConstructor(itClassSymbol: IrClassSymbol): IrConstructor? =
        currentlyGeneratedDelegatedConstructors[itClassSymbol]

    fun containingFileIfAny(): IrFile? = parentStack.getOrNull(0) as? IrFile

    inline fun withContainingFirClass(containingFirClass: FirClass, f: () -> Unit) {
        containingFirClassStack += containingFirClass
        try {
            f()
        } finally {
            containingFirClassStack.removeAt(containingFirClassStack.size - 1)
        }
    }

    fun parentFromStack(): IrDeclarationParent = parentStack.last()

    fun parentAccessorOfPropertyFromStack(propertySymbol: IrPropertySymbol): IrSimpleFunction {
        // It is safe to access an owner of property symbol here, because this function may be called
        // only from property accessor of corresponding property
        // We inside accessor -> accessor is built -> property is built
        @OptIn(IrSymbolInternals::class)
        val property = propertySymbol.owner
        for (parent in parentStack.asReversed()) {
            when (parent) {
                property.getter -> return parent as IrSimpleFunction
                property.setter -> return parent as IrSimpleFunction
            }
        }
        error("Accessor of property ${property.render()} not found on parent stack")
    }

    @OptIn(IrSymbolInternals::class)
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    inline fun <reified D : IrDeclaration> findDeclarationInParentsStack(symbol: IrSymbol): @kotlin.internal.NoInfer D {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            return symbol.owner as D
        }
        for (parent in parentStack.asReversed()) {
            if ((parent as? IrDeclaration)?.symbol == symbol) {
                return parent as D
            }
        }
        /*
         * In case of IDE (when allowNonCachedDeclarations is set to true) we may be in scope of some already compiled class,
         *   for which we have Fir2IrLazyClass in symbol
         */
        if (configuration.allowNonCachedDeclarations) {
            return symbol.owner as D
        }
        error("Declaration with symbol $symbol is not found in parents stack")
    }

    fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T {
        declaration.parent = parentStack.last()
        return declaration
    }

    fun containerFirClass(): FirClass? = containingFirClassStack.lastOrNull()

    @PublishedApi
    @PrivateForInline
    internal val functionStack = mutableListOf<IrFunction>()

    inline fun <T : IrFunction, R> withFunction(function: T, f: T.() -> R): R {
        functionStack += function
        try {
            return function.f()
        } finally {
            functionStack.removeAt(functionStack.size - 1)
        }
    }

    @PublishedApi
    @PrivateForInline
    internal val propertyStack = mutableListOf<Pair<IrProperty, FirProperty?>>()

    inline fun <R> withProperty(property: IrProperty, firProperty: FirProperty? = null, f: IrProperty.() -> R): R {
        propertyStack += (property to firProperty)
        try {
            return property.f()
        } finally {
            propertyStack.removeAt(propertyStack.size - 1)
        }
    }

    @PublishedApi
    @PrivateForInline
    internal val classStack = mutableListOf<IrClass>()

    inline fun <R> withClass(klass: IrClass, f: IrClass.() -> R): R {
        classStack += klass
        return try {
            klass.f()
        } finally {
            classStack.removeAt(classStack.size - 1)
        }
    }

    @PublishedApi
    @PrivateForInline
    internal val whenSubjectVariableStack = mutableListOf<IrVariable>()

    @PublishedApi
    @PrivateForInline
    internal val safeCallSubjectVariableStack = mutableListOf<IrVariable>()

    inline fun <T> withWhenSubject(subject: IrVariable?, f: () -> T): T {
        if (subject != null) whenSubjectVariableStack += subject
        try {
            return f()
        } finally {
            if (subject != null) whenSubjectVariableStack.removeAt(whenSubjectVariableStack.size - 1)
        }
    }

    inline fun <T> withSafeCallSubject(subject: IrVariable?, f: () -> T): T {
        if (subject != null) safeCallSubjectVariableStack += subject
        try {
            return f()
        } finally {
            if (subject != null) safeCallSubjectVariableStack.removeAt(safeCallSubjectVariableStack.size - 1)
        }
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
