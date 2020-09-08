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
import org.jetbrains.kotlin.fir.symbols.PossiblyFirFakeOverrideSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
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
                createFakeOverriddenIfNeeded(
                    klass, this, isLocal, functionSymbol,
                    declarationStorage::getCachedIrFunction,
                    declarationStorage::createIrFunction,
                    createFakeOverrideSymbol = { firFunction, callableSymbol ->
                        FirClassSubstitutionScope.createFakeOverrideFunction(
                            session, firFunction, callableSymbol,
                            derivedClassId = klass.symbol.classId,
                            isExpect = (klass as? FirRegularClass)?.isExpect == true
                        )
                    },
                    baseFunctionSymbols,
                    result,
                    containsErrorTypes = { irFunction ->
                        irFunction.returnType.containsErrorType() || irFunction.valueParameters.any { it.type.containsErrorType() }
                    }
                )
            }

            useSiteMemberScope.processPropertiesByName(name) { propertySymbol ->
                createFakeOverriddenIfNeeded(
                    klass, this, isLocal, propertySymbol,
                    declarationStorage::getCachedIrProperty,
                    declarationStorage::createIrProperty,
                    createFakeOverrideSymbol = { firProperty, callableSymbol ->
                        FirClassSubstitutionScope.createFakeOverrideProperty(
                            session, firProperty, callableSymbol,
                            derivedClassId = klass.symbol.classId,
                            isExpect = (klass as? FirRegularClass)?.isExpect == true
                        )
                    },
                    basePropertySymbols,
                    result,
                    containsErrorTypes = { irProperty ->
                        irProperty.backingField?.type?.containsErrorType() == true ||
                                irProperty.getter?.returnType?.containsErrorType() == true
                    }
                )
            }
        }
        return result
    }

    private inline fun <reified D : FirCallableMemberDeclaration<D>, reified S, reified I : IrDeclaration> createFakeOverriddenIfNeeded(
        klass: FirClass<*>,
        irClass: IrClass,
        isLocal: Boolean,
        originalSymbol: FirCallableSymbol<*>,
        cachedIrDeclaration: (D) -> I?,
        createIrDeclaration: (D, irParent: IrClass, thisReceiverOwner: IrClass?, origin: IrDeclarationOrigin, isLocal: Boolean) -> I,
        createFakeOverrideSymbol: (D, S) -> S,
        baseSymbols: MutableMap<I, S>,
        result: MutableList<in I>,
        containsErrorTypes: (I) -> Boolean
    ) where S : FirCallableSymbol<D>, S : PossiblyFirFakeOverrideSymbol<D, S> {
        if (originalSymbol !is S) return
        val originalDeclaration = originalSymbol.fir
        val origin = IrDeclarationOrigin.FAKE_OVERRIDE
        val baseSymbol = originalSymbol.deepestOverriddenSymbol() as S
        if (originalSymbol.isFakeOverride && originalSymbol.callableId.classId == klass.symbol.classId) {
            // Substitution case
            // NB: see comment above about substituted function' parent
            val irDeclaration = cachedIrDeclaration(originalDeclaration)?.takeIf { it.parent == irClass }
                ?: createIrDeclaration(
                    originalDeclaration, irClass,
                    declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                    origin,
                    isLocal
                )
            irDeclaration.parent = irClass
            baseSymbols[irDeclaration] = baseSymbol
            result += irDeclaration
        } else if (originalDeclaration.allowsToHaveFakeOverrideIn(klass)) {
            // Trivial fake override case
            val fakeOverrideSymbol = createFakeOverrideSymbol(
                originalDeclaration, baseSymbol
            )

            classifierStorage.preCacheTypeParameters(originalDeclaration)
            val irDeclaration = createIrDeclaration(
                fakeOverrideSymbol.fir, irClass,
                declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                origin,
                isLocal
            )
            if (containsErrorTypes(irDeclaration)) {
                return
            }
            irDeclaration.parent = irClass
            baseSymbols[irDeclaration] = baseSymbol
            result += irDeclaration
        }
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

