/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.visibility
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.Name

class Fir2IrFakeOverrideGenerator(
    private val session: FirSession,
    private val declarationStorage: Fir2IrDeclarationStorage,
    private val conversionScope: Fir2IrConversionScope,
    private val fakeOverrideMode: FakeOverrideMode
) {

    private fun IrSimpleFunction.withFunction(f: IrSimpleFunction.() -> Unit): IrSimpleFunction {
        return conversionScope.withFunction(conversionScope.applyParentFromStackTo(this), f)
    }

    private fun IrProperty.withProperty(f: IrProperty.() -> Unit): IrProperty {
        return conversionScope.withProperty(conversionScope.applyParentFromStackTo(this), f)
    }

    private fun IrProperty.setOverriddenSymbolsForAccessors(
        property: FirProperty,
        firOverriddenSymbol: FirPropertySymbol
    ): IrProperty {
        val firOverriddenProperty = firOverriddenSymbol.fir
        val overriddenProperty = declarationStorage.getIrProperty(
            firOverriddenProperty, declarationStorage.findIrParent(firOverriddenProperty)
        )
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

    fun IrClass.addFakeOverrides(klass: FirClass<*>, processedCallableNames: MutableList<Name>) {
        if (fakeOverrideMode == FakeOverrideMode.NONE) return
        val superTypesCallableNames = klass.collectCallableNamesFromSupertypes(session)
        val useSiteMemberScope = (klass as? FirRegularClass)?.buildUseSiteMemberScope(session, ScopeSession()) ?: return
        for (name in superTypesCallableNames) {
            if (name in processedCallableNames) continue
            processedCallableNames += name
            useSiteMemberScope.processFunctionsByName(name) { functionSymbol ->
                if (functionSymbol is FirNamedFunctionSymbol) {
                    val originalFunction = functionSymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    if (functionSymbol.isFakeOverride) {
                        // Substitution case
                        val baseSymbol = functionSymbol.deepestOverriddenSymbol() as FirNamedFunctionSymbol
                        val irFunction = declarationStorage.getIrFunction(
                            // TODO: parents for functions and properties should be consistent
                            originalFunction, declarationStorage.findIrParent(baseSymbol.fir),
                            origin = origin, shouldLeaveScope = true
                        )
                        // In fake overrides, parent logic is a bit specific, because
                        // parent of *original* function (base class) is used for dispatch receiver,
                        // but fake override itself uses parent from its containing (derived) class
                        val overriddenSymbol = declarationStorage.getIrFunctionSymbol(baseSymbol) as IrSimpleFunctionSymbol
                        declarations += irFunction.withFunction {
                            overriddenSymbols = listOf(overriddenSymbol)
                        }
                    } else if (fakeOverrideMode != FakeOverrideMode.SUBSTITUTION && originalFunction.visibility != Visibilities.PRIVATE) {
                        // Trivial fake override case
                        val fakeOverrideSymbol = FirClassSubstitutionScope.createFakeOverrideFunction(
                            session, originalFunction, functionSymbol
                        )
                        val fakeOverrideFunction = fakeOverrideSymbol.fir

                        val irFunction = declarationStorage.getIrFunction(
                            fakeOverrideFunction, declarationStorage.findIrParent(originalFunction),
                            origin = origin, shouldLeaveScope = true
                        )
                        val overriddenSymbol = declarationStorage.getIrFunctionSymbol(functionSymbol) as IrSimpleFunctionSymbol
                        declarations += irFunction.withFunction {
                            overriddenSymbols = listOf(overriddenSymbol)
                        }
                    }
                }
            }
            useSiteMemberScope.processPropertiesByName(name) { propertySymbol ->
                if (propertySymbol is FirPropertySymbol) {
                    val originalProperty = propertySymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    if (propertySymbol.isFakeOverride) {
                        // Substitution case
                        val baseSymbol = propertySymbol.deepestOverriddenSymbol() as FirPropertySymbol
                        val irProperty = declarationStorage.getIrProperty(
                            originalProperty, irParent = this, origin = origin
                        )
                        declarations += irProperty.withProperty {
                            setOverriddenSymbolsForAccessors(originalProperty, firOverriddenSymbol = baseSymbol)
                        }
                    } else if (fakeOverrideMode != FakeOverrideMode.SUBSTITUTION && originalProperty.visibility != Visibilities.PRIVATE) {
                        // Trivial fake override case
                        val fakeOverrideSymbol = FirClassSubstitutionScope.createFakeOverrideProperty(
                            session, originalProperty, propertySymbol, derivedClassId = klass.symbol.classId
                        )
                        val fakeOverrideProperty = fakeOverrideSymbol.fir

                        val irProperty = declarationStorage.getIrProperty(
                            fakeOverrideProperty, irParent = this, origin = origin
                        )
                        declarations += irProperty.withProperty {
                            setOverriddenSymbolsForAccessors(fakeOverrideProperty, firOverriddenSymbol = propertySymbol)
                        }
                    }
                }
            }
        }
    }

}