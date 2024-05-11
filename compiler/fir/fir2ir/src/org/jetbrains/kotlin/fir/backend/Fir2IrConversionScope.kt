/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.PrivateForInline

@OptIn(PrivateForInline::class)
class Fir2IrConversionScope(val configuration: Fir2IrConfiguration) {
    @PublishedApi
    @PrivateForInline
    internal val _parentStack: MutableList<IrDeclarationParent> = mutableListOf()

    val parentStack: List<IrDeclarationParent>
        get() = _parentStack

    @PublishedApi
    @PrivateForInline
    internal val scopeStack: MutableList<Scope> = mutableListOf()

    @PublishedApi
    @PrivateForInline
    internal val containingFirClassStack: MutableList<FirClass> = mutableListOf()

    @PublishedApi
    @PrivateForInline
    internal val currentlyGeneratedDelegatedConstructors: MutableMap<IrClassSymbol, IrConstructor> = mutableMapOf()

    inline fun <T : IrDeclarationParent, R> withParent(parent: T, f: T.() -> R): R {
        _parentStack += parent
        if (parent is IrDeclaration) {
            scopeStack += Scope(parent.symbol)
        }
        try {
            return parent.f()
        } finally {
            if (parent is IrDeclaration) {
                scopeStack.removeAt(scopeStack.size - 1)
            }
            _parentStack.removeAt(_parentStack.size - 1)
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

    fun containingFileIfAny(): IrFile? = _parentStack.getOrNull(0) as? IrFile

    inline fun withContainingFirClass(containingFirClass: FirClass, f: () -> Unit) {
        containingFirClassStack += containingFirClass
        try {
            f()
        } finally {
            containingFirClassStack.removeAt(containingFirClassStack.size - 1)
        }
    }

    fun parentFromStack(): IrDeclarationParent = _parentStack.last()

    fun scope(): Scope = scopeStack.last()

    fun parentAccessorOfPropertyFromStack(propertySymbol: IrPropertySymbol): IrSimpleFunction {
        // It is safe to access an owner of property symbol here, because this function may be called
        // only from property accessor of corresponding property
        // We inside accessor -> accessor is built -> property is built
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val property = propertySymbol.owner
        for (parent in _parentStack.asReversed()) {
            when (parent) {
                property.getter -> return parent as IrSimpleFunction
                property.setter -> return parent as IrSimpleFunction
            }
        }
        error("Accessor of property ${property.render()} not found on parent stack")
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    inline fun <reified D : IrDeclaration> findDeclarationInParentsStack(symbol: IrSymbol): @kotlin.internal.NoInfer D {
        // This is an unsafe fast path for production
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            return symbol.owner as D
        }
        // With slow assertions the following code guarantees that taking owner from symbol is safe
        for (parent in _parentStack.asReversed()) {
            if ((parent as? IrDeclaration)?.symbol == symbol) {
                return parent as D
            }
        }
        /*
         * In case of IDE (when allowNonCachedDeclarations is set to true) we may be in scope of some already compiled class,
         *   for which we have Fir2IrLazyClass in symbol
         */
        if (configuration.allowNonCachedDeclarations) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            return symbol.owner as D
        }
        error("Declaration with symbol $symbol is not found in parents stack")
    }

    fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T {
        declaration.parent = _parentStack.last()
        return declaration
    }

    fun containerFirClass(): FirClass? = containingFirClassStack.lastOrNull()

    @PublishedApi
    @PrivateForInline
    internal val functionStack: MutableList<IrFunction> = mutableListOf()

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
    internal val propertyStack: MutableList<Pair<IrProperty, FirProperty?>> = mutableListOf()

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
    internal val classStack: MutableList<IrClass> = mutableListOf()

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
    internal val whenSubjectVariableStack: MutableList<IrVariable> = mutableListOf()

    @PublishedApi
    @PrivateForInline
    internal val safeCallSubjectVariableStack: MutableList<IrVariable> = mutableListOf()

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

    fun returnTarget(expression: FirReturnExpression, declarationStorage: Fir2IrDeclarationStorage): IrFunctionSymbol {
        val irTarget = when (val firTarget = expression.target.labeledElement) {
            is FirConstructor -> declarationStorage.getCachedIrConstructorSymbol(firTarget)
            is FirPropertyAccessor -> {
                var answer: IrFunctionSymbol? = null
                for ((property, firProperty) in propertyStack.asReversed()) {
                    if (firProperty?.getter === firTarget) {
                        answer = property.getter?.symbol
                    } else if (firProperty?.setter === firTarget) {
                        answer = property.setter?.symbol
                    }
                }
                answer
            }
            else -> declarationStorage.getCachedIrFunctionSymbol(firTarget)
        }
        for (potentialTarget in functionStack.asReversed()) {
            val targetSymbol = potentialTarget.symbol
            if (targetSymbol == irTarget) {
                return targetSymbol
            }
        }
        return functionStack.last().symbol
    }

    fun parent(): IrDeclarationParent? = _parentStack.lastOrNull()

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

    fun lastWhenSubject(): IrVariable = whenSubjectVariableStack.last()
    fun lastSafeCallSubject(): IrVariable = safeCallSubjectVariableStack.last()

    fun shouldEraseType(type: ConeTypeParameterType): Boolean = containingFirClassStack.asReversed().any { clazz ->
        if (clazz !is FirAnonymousObject && !clazz.isLocal) return@any false

        val typeParameterSymbol = type.lookupTag.typeParameterSymbol
        if (typeParameterSymbol.containingDeclarationSymbol.fir.let { it !is FirProperty || it.delegate == null || !it.isExtension }) {
            return@any false
        }

        return@any clazz.typeParameters.any { it.symbol === typeParameterSymbol }
    }

    @PublishedApi
    @PrivateForInline
    internal val _initBlocksStack: MutableList<IrAnonymousInitializer> = mutableListOf()
    internal val initBlocksStack: List<IrAnonymousInitializer>
        get() = _initBlocksStack

    inline fun <T> withInitBlock(initializer: IrAnonymousInitializer, f: () -> T): T {
        _initBlocksStack += initializer
        try {
            return f()
        } finally {
            _initBlocksStack.removeAt(_initBlocksStack.size - 1)
        }
    }
}
