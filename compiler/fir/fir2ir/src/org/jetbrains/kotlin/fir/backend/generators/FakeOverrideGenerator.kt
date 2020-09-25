/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
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
import org.jetbrains.kotlin.name.ClassId

class FakeOverrideGenerator(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val classifierStorage: Fir2IrClassifierStorage,
    private val declarationStorage: Fir2IrDeclarationStorage,
    private val conversionScope: Fir2IrConversionScope
) {

    private val baseFunctionSymbols = mutableMapOf<IrFunction, List<FirNamedFunctionSymbol>>()
    private val basePropertySymbols = mutableMapOf<IrProperty, List<FirPropertySymbol>>()

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

    fun IrClass.addFakeOverrides(klass: FirClass<*>, declarations: Collection<FirDeclaration>) {
        this.declarations += getFakeOverrides(
            klass,
            declarations
        )
    }

    fun IrClass.getFakeOverrides(klass: FirClass<*>, realDeclarations: Collection<FirDeclaration>): List<IrDeclaration> {
        val result = mutableListOf<IrDeclaration>()
        val useSiteMemberScope = klass.unsubstitutedScope(session, scopeSession)
        val superTypesCallableNames = useSiteMemberScope.getCallableNames()
        val realDeclarationSymbols = realDeclarations.filterIsInstance<FirSymbolOwner<*>>().mapTo(mutableSetOf(), FirSymbolOwner<*>::symbol)
        for (name in superTypesCallableNames) {
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
                    },
                    realDeclarationSymbols,
                    FirTypeScope::getDirectOverriddenFunctions,
                    useSiteMemberScope,
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
                    },
                    realDeclarationSymbols,
                    FirTypeScope::getDirectOverriddenProperties,
                    useSiteMemberScope,
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
        baseSymbols: MutableMap<I, List<S>>,
        result: MutableList<in I>,
        containsErrorTypes: (I) -> Boolean,
        realDeclarationSymbols: Set<AbstractFirBasedSymbol<*>>,
        computeDirectOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirTypeScope,
    ) where S : FirCallableSymbol<D>, S : PossiblyFirFakeOverrideSymbol<D, S> {
        if (originalSymbol !is S || originalSymbol in realDeclarationSymbols) return
        val classId = klass.symbol.classId
        val originalDeclaration = originalSymbol.fir
        if (originalSymbol.callableId.classId == classId && !originalDeclaration.origin.fromSupertypes) return

        val origin = IrDeclarationOrigin.FAKE_OVERRIDE
        val baseSymbol = originalSymbol.deepestOverriddenSymbol() as S

        if ((originalSymbol.isFakeOverride || originalSymbol.isIntersectionOverride) &&
            originalSymbol.callableId.classId == classId
        ) {
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
            baseSymbols[irDeclaration] = computeBaseSymbols(originalSymbol, baseSymbol, computeDirectOverridden, scope, classId)
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
            baseSymbols[irDeclaration] = computeBaseSymbols(originalSymbol, baseSymbol, computeDirectOverridden, scope, classId)
            result += irDeclaration
        }
    }

    private inline fun <S : FirCallableSymbol<*>> computeBaseSymbols(
        symbol: S,
        basedSymbol: S,
        directOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirTypeScope,
        containingClassId: ClassId,
    ): List<S> {
        if (!symbol.isIntersectionOverride) return listOf(basedSymbol)
        return scope.directOverridden(symbol).map {
            @Suppress("UNCHECKED_CAST")
            if (it is PossiblyFirFakeOverrideSymbol<*, *> && it.isFakeOverride && it.callableId.classId == containingClassId)
                it.overriddenSymbol!! as S
            else
                it
        }
    }

    fun bindOverriddenSymbols(declarations: List<IrDeclaration>) {
        for (declaration in declarations) {
            if (declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    val baseSymbols =
                        baseFunctionSymbols[declaration]!!.map { declarationStorage.getIrFunctionSymbol(it) as IrSimpleFunctionSymbol }
                    declaration.withFunction {
                        overriddenSymbols = baseSymbols
                    }
                }
                is IrProperty -> {
                    val baseSymbols = basePropertySymbols[declaration]!!
                    declaration.withProperty {
                        discardAccessorsAccordingToBaseVisibility(baseSymbols)
                        setOverriddenSymbolsForAccessors(declarationStorage, declaration.isVar, baseSymbols)
                    }
                }
            }
        }
    }

    private fun IrProperty.discardAccessorsAccordingToBaseVisibility(baseSymbols: List<FirPropertySymbol>) {
        for (baseSymbol in baseSymbols) {
            // Do not create fake overrides for accessors if not allowed to do so, e.g., private lateinit var.
            if (baseSymbol.fir.getter?.allowsToHaveFakeOverride != true) {
                getter = null
            }
            // or private setter
            if (baseSymbol.fir.setter?.allowsToHaveFakeOverride != true) {
                setter = null
            }
        }
    }

    private fun IrProperty.setOverriddenSymbolsForAccessors(
        declarationStorage: Fir2IrDeclarationStorage,
        isVar: Boolean,
        firOverriddenSymbols: List<FirPropertySymbol>
    ): IrProperty {
        val overriddenIrProperties = firOverriddenSymbols.mapNotNull {
            (declarationStorage.getIrPropertySymbol(it) as? IrPropertySymbol)?.owner
        }
        getter?.apply {
            overriddenSymbols = overriddenIrProperties.mapNotNull { it.getter?.symbol }
        }
        if (isVar) {
            setter?.apply {
                overriddenSymbols = overriddenIrProperties.mapNotNull { it.setter?.symbol }
            }
        }
        return this
    }
}

