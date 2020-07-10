/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.collectCallableNamesFromSupertypes
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name

class FakeOverrideGenerator(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val classifierStorage: Fir2IrClassifierStorage,
    private val declarationStorage: Fir2IrDeclarationStorage,
    private val conversionScope: Fir2IrConversionScope,
    private val fakeOverrideMode: FakeOverrideMode
) {

    private fun IrSimpleFunction.withFunction(f: IrSimpleFunction.() -> Unit): IrSimpleFunction {
        return conversionScope.withFunction(this, f)
    }

    private fun IrProperty.withProperty(f: IrProperty.() -> Unit): IrProperty {
        return conversionScope.withProperty(this, f)
    }

    private fun FirCallableMemberDeclaration<*>.allowsToHaveFakeOverrideIn(klass: FirClass<*>): Boolean {
        if (!allowsToHaveFakeOverride) return false
        if (this.visibility != JavaVisibilities.PACKAGE_VISIBILITY) return true
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
        if (fakeOverrideMode == FakeOverrideMode.NONE) return emptyList()
        val superTypesCallableNames = klass.collectCallableNamesFromSupertypes(session)
        val useSiteMemberScope = klass.buildUseSiteMemberScope(session, scopeSession) ?: return emptyList()
        for (name in superTypesCallableNames) {
            if (name in processedCallableNames) continue
            processedCallableNames += name
            useSiteMemberScope.processFunctionsByName(name) { functionSymbol ->
                if (functionSymbol is FirNamedFunctionSymbol) {
                    val originalFunction = functionSymbol.fir
                    if (originalFunction.isStatic && originalFunction.name in Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES) {
                        return@processFunctionsByName
                    }
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    val baseSymbol = functionSymbol.deepestOverriddenSymbol() as FirNamedFunctionSymbol
                    if (functionSymbol.isFakeOverride &&
                        (functionSymbol.callableId.classId == klass.symbol.classId || fakeOverrideMode == FakeOverrideMode.SUBSTITUTION)
                    ) {
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
                                origin = origin
                            )
                        // In fake overrides, parent logic is a bit specific, because
                        // parent of *original* function (base class) is used for dispatch receiver,
                        // but fake override itself uses parent from its containing (derived) class
                        val overriddenSymbol = declarationStorage.getIrFunctionSymbol(baseSymbol) as IrSimpleFunctionSymbol
                        irFunction.parent = this
                        result += irFunction.withFunction {
                            overriddenSymbols = listOf(overriddenSymbol)
                        }
                    } else if (fakeOverrideMode != FakeOverrideMode.SUBSTITUTION && originalFunction.allowsToHaveFakeOverrideIn(klass)) {
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
                            origin = origin
                        )
                        if (irFunction.returnType.containsErrorType() || irFunction.valueParameters.any { it.type.containsErrorType() }) {
                            return@processFunctionsByName
                        }
                        val overriddenSymbol = declarationStorage.getIrFunctionSymbol(baseSymbol) as IrSimpleFunctionSymbol
                        irFunction.parent = this
                        result += irFunction.withFunction {
                            overriddenSymbols = listOf(overriddenSymbol)
                        }
                    }
                }
            }
            useSiteMemberScope.processPropertiesByName(name) { propertySymbol ->
                if (propertySymbol is FirPropertySymbol) {
                    val originalProperty = propertySymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    val baseSymbol = propertySymbol.deepestOverriddenSymbol() as FirPropertySymbol
                    if (propertySymbol.isFakeOverride &&
                        (propertySymbol.callableId.classId == klass.symbol.classId || fakeOverrideMode == FakeOverrideMode.SUBSTITUTION)
                    ) {
                        // Substitution case
                        // NB: see comment above about substituted function' parent
                        val irProperty = declarationStorage.getCachedIrProperty(originalProperty)?.takeIf { it.parent == this }
                            ?: declarationStorage.createIrProperty(
                                originalProperty, irParent = this,
                                thisReceiverOwner = declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                                origin = origin
                            )
                        irProperty.parent = this
                        result += irProperty.withProperty {
                            discardAccessorsAccordingToBaseVisibility(baseSymbol)
                            setOverriddenSymbolsForAccessors(declarationStorage, originalProperty, firOverriddenSymbol = baseSymbol)
                        }
                    } else if (fakeOverrideMode != FakeOverrideMode.SUBSTITUTION && originalProperty.allowsToHaveFakeOverrideIn(klass)) {
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
                            origin = origin
                        )
                        if (irProperty.backingField?.type?.containsErrorType() == true ||
                            irProperty.getter?.returnType?.containsErrorType() == true
                        ) {
                            return@processPropertiesByName
                        }
                        irProperty.parent = this
                        result += irProperty.withProperty {
                            discardAccessorsAccordingToBaseVisibility(baseSymbol)
                            setOverriddenSymbolsForAccessors(declarationStorage, fakeOverrideProperty, firOverriddenSymbol = baseSymbol)
                        }
                    }
                }
            }
        }
        return result
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
        property: FirProperty,
        firOverriddenSymbol: FirPropertySymbol
    ): IrProperty {
        val irSymbol = declarationStorage.getIrPropertyOrFieldSymbol(firOverriddenSymbol) as? IrPropertySymbol ?: return this
        val overriddenProperty = irSymbol.owner
        getter?.apply {
            overriddenProperty.getter?.symbol?.let { overriddenSymbols = listOf(it) }
        }
        if (property.isVar) {
            setter?.apply {
                overriddenProperty.setter?.symbol?.let { overriddenSymbols = listOf(it) }
            }
        }
        return this
    }
}

