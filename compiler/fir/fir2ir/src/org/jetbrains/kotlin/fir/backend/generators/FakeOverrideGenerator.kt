/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name

class FakeOverrideGenerator(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val classifierStorage: Fir2IrClassifierStorage,
    private val declarationStorage: Fir2IrDeclarationStorage,
    private val conversionScope: Fir2IrConversionScope
) {

    private val baseFunctionSymbols = mutableMapOf<IrFunction, FirNamedFunctionSymbol>()
    private val basePropertySymbols = mutableMapOf<IrProperty, FirPropertySymbol>()

    private fun IrSimpleFunction.withFunction(f: IrSimpleFunction.() -> Unit): IrSimpleFunction {
        return conversionScope.withFunction(this, f)
    }

    private fun IrProperty.withProperty(f: IrProperty.() -> Unit): IrProperty {
        return conversionScope.withProperty(this, f)
    }

    private fun FirCallableMemberDeclaration<*>.allowsToHaveFakeOverrideIn(klass: FirClass<*>): Boolean {
        if (!allowsToHaveFakeOverride) return false
        if (this.visibility != JavaDescriptorVisibilities.PACKAGE_VISIBILITY) return true
        return this.symbol.callableId.packageName == klass.symbol.classId.packageFqName
    }

    private fun IrType.containsErrorType(): Boolean {
        return when (this) {
            is IrErrorType -> true
            is IrSimpleType -> arguments.any { it is IrTypeProjection && it.type.containsErrorType() }
            else -> false
        }
    }

    fun IrClass.addFakeOverrides(klass: FirClass<*>, processedCallableNames: MutableSet<Name>) {
        declarations += getFakeOverrides(klass, processedCallableNames)
    }

    fun IrClass.getFakeOverrides(klass: FirClass<*>, processedCallableNames: MutableSet<Name>): List<IrDeclaration> {
        val result = mutableListOf<IrDeclaration>()
        val superTypesCallableNames = klass.collectCallableNamesFromSupertypes(session)
        val useSiteMemberScope = klass.unsubstitutedScope(session, scopeSession)
        for (name in superTypesCallableNames) {
            if (name in processedCallableNames) continue
            processedCallableNames += name
            val isLocal = klass !is FirRegularClass || klass.isLocal
            useSiteMemberScope.processFunctionsByName(name) { functionSymbol ->
                if (functionSymbol is FirNamedFunctionSymbol) {
                    val originalFunction = functionSymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    val baseSymbol = functionSymbol.deepestOverriddenSymbol() as FirNamedFunctionSymbol
                    if (functionSymbol.isFakeOverride && functionSymbol.callableId.classId == klass.symbol.classId) {
                        // Substitution case
                        // NB: we can get same substituted FIR fake override in a different class, if it derives the same genetic type
                        // open class Base<T> {
                        //     fun foo(): T
                        // }
                        // class Derived1 : Base<String>() {}
                        // class Derived2 : Base<String>() {}
                        // That's why we must check parent during caching...
                        val irFunction = declarationStorage.getCachedIrFunction(originalFunction)?.takeIf { it.parent == this }
                            ?: declarationStorage.createIrFunction(
                                originalFunction,
                                irParent = this,
                                thisReceiverOwner = declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                                origin = origin,
                                isLocal = isLocal
                            )
                        // In fake overrides, parent logic is a bit specific, because
                        // parent of *original* function (base class) is used for dispatch receiver,
                        // but fake override itself uses parent from its containing (derived) class
                        irFunction.parent = this
                        baseFunctionSymbols[irFunction] = baseSymbol
                        result += irFunction
                    } else if (originalFunction.allowsToHaveFakeOverrideIn(klass)) {
                        // Trivial fake override case
                        val fakeOverrideSymbol = FirClassSubstitutionScope.createFakeOverrideFunction(
                            session, originalFunction, baseSymbol,
                            derivedClassId = klass.symbol.classId,
                            isExpect = (klass as? FirRegularClass)?.isExpect == true
                        )
                        val fakeOverrideFunction = fakeOverrideSymbol.fir

                        classifierStorage.preCacheTypeParameters(originalFunction)
                        val irFunction = declarationStorage.createIrFunction(
                            fakeOverrideFunction,
                            irParent = this,
                            thisReceiverOwner = declarationStorage.findIrParent(originalFunction) as? IrClass,
                            origin = origin,
                            isLocal = isLocal
                        )
                        if (irFunction.returnType.containsErrorType() || irFunction.valueParameters.any { it.type.containsErrorType() }) {
                            return@processFunctionsByName
                        }
                        irFunction.parent = this
                        baseFunctionSymbols[irFunction] = baseSymbol
                        result += irFunction
                    }
                }
            }
            useSiteMemberScope.processPropertiesByName(name) { propertySymbol ->
                if (propertySymbol is FirPropertySymbol) {
                    val originalProperty = propertySymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    val baseSymbol = propertySymbol.deepestOverriddenSymbol() as FirPropertySymbol
                    if (propertySymbol.isFakeOverride && propertySymbol.callableId.classId == klass.symbol.classId) {
                        // Substitution case
                        // NB: see comment above about substituted function' parent
                        val irProperty = declarationStorage.getCachedIrProperty(originalProperty)?.takeIf { it.parent == this }
                            ?: declarationStorage.createIrProperty(
                                originalProperty, irParent = this,
                                thisReceiverOwner = declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                                origin = origin,
                                isLocal = isLocal
                            )
                        irProperty.parent = this
                        basePropertySymbols[irProperty] = baseSymbol
                        result += irProperty
                    } else if (originalProperty.allowsToHaveFakeOverrideIn(klass)) {
                        // Trivial fake override case
                        val fakeOverrideSymbol = FirClassSubstitutionScope.createFakeOverrideProperty(
                            session, originalProperty, baseSymbol,
                            derivedClassId = klass.symbol.classId,
                            isExpect = (klass as? FirRegularClass)?.isExpect == true
                        )
                        val fakeOverrideProperty = fakeOverrideSymbol.fir

                        classifierStorage.preCacheTypeParameters(originalProperty)
                        val irProperty = declarationStorage.createIrProperty(
                            fakeOverrideProperty, irParent = this,
                            thisReceiverOwner = declarationStorage.findIrParent(originalProperty) as? IrClass,
                            origin = origin,
                            isLocal = isLocal
                        )
                        if (irProperty.backingField?.type?.containsErrorType() == true ||
                            irProperty.getter?.returnType?.containsErrorType() == true
                        ) {
                            return@processPropertiesByName
                        }
                        irProperty.parent = this
                        basePropertySymbols[irProperty] = baseSymbol
                        result += irProperty
                    }
                }
            }
        }
        return result
    }

    fun bindOverriddenSymbols(declarations: List<IrDeclaration>) {
        for (declaration in declarations) {
            if (declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    val baseSymbol = baseFunctionSymbols[declaration]!!
                    val overriddenSymbol = declarationStorage.getIrFunctionSymbol(baseSymbol) as IrSimpleFunctionSymbol
                    declaration.withFunction {
                        overriddenSymbols = listOf(overriddenSymbol)
                    }
                }
                is IrProperty -> {
                    val baseSymbol = basePropertySymbols[declaration]!!
                    declaration.withProperty {
                        discardAccessorsAccordingToBaseVisibility(baseSymbol)
                        setOverriddenSymbolsForAccessors(declarationStorage, declaration.isVar, firOverriddenSymbol = baseSymbol)
                    }
                }
            }
        }
    }

    private fun IrProperty.discardAccessorsAccordingToBaseVisibility(baseSymbol: FirPropertySymbol) {
        // Do not create fake overrides for accessors if not allowed to do so, e.g., private lateinit var.
        if (baseSymbol.fir.getter?.allowsToHaveFakeOverride != true) {
            getter = null
        }
        // or private setter
        if (baseSymbol.fir.setter?.allowsToHaveFakeOverride != true) {
            setter = null
        }
    }

    private fun IrProperty.setOverriddenSymbolsForAccessors(
        declarationStorage: Fir2IrDeclarationStorage,
        isVar: Boolean,
        firOverriddenSymbol: FirPropertySymbol
    ): IrProperty {
        val irSymbol = declarationStorage.getIrPropertySymbol(firOverriddenSymbol) as? IrPropertySymbol ?: return this
        val overriddenProperty = irSymbol.owner
        getter?.apply {
            overriddenProperty.getter?.symbol?.let { overriddenSymbols = listOf(it) }
        }
        if (isVar) {
            setter?.apply {
                overriddenProperty.setter?.symbol?.let { overriddenSymbols = listOf(it) }
            }
        }
        return this
    }
}

