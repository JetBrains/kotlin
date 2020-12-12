/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
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
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities

class FakeOverrideGenerator(
    private val components: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

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
        val useSiteMemberScope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
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
                        FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
                            session, firFunction, callableSymbol,
                            newDispatchReceiverType = klass.defaultType(),
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
                        FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
                            session, firProperty, callableSymbol,
                            newDispatchReceiverType = klass.defaultType(),
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

    private inline fun <reified D : FirCallableMemberDeclaration<D>, reified S : FirCallableSymbol<D>, reified I : IrDeclaration> createFakeOverriddenIfNeeded(
        klass: FirClass<*>,
        irClass: IrClass,
        isLocal: Boolean,
        originalSymbol: FirCallableSymbol<*>,
        cachedIrDeclaration: (D, (D) -> IdSignature?) -> I?,
        createIrDeclaration: (D, irParent: IrClass, thisReceiverOwner: IrClass?, origin: IrDeclarationOrigin, isLocal: Boolean) -> I,
        createFakeOverrideSymbol: (D, S) -> S,
        baseSymbols: MutableMap<I, List<S>>,
        result: MutableList<in I>,
        containsErrorTypes: (I) -> Boolean,
        realDeclarationSymbols: Set<AbstractFirBasedSymbol<*>>,
        computeDirectOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirTypeScope,
    ) {
        if (originalSymbol !is S || originalSymbol in realDeclarationSymbols) return
        val classLookupTag = klass.symbol.toLookupTag()
        val originalDeclaration = originalSymbol.fir
        if (originalSymbol.dispatchReceiverClassOrNull() == classLookupTag && !originalDeclaration.origin.fromSupertypes) return
        if (originalDeclaration.visibility == Visibilities.Private) return

        val origin = IrDeclarationOrigin.FAKE_OVERRIDE
        val baseSymbol = originalSymbol.unwrapSubstitutionAndIntersectionOverrides() as S
        val baseDeclaration = when {
            originalSymbol.fir.origin.fromSupertypes && originalSymbol.dispatchReceiverClassOrNull() == classLookupTag -> {
                // Substitution case
                originalDeclaration
            }
            originalDeclaration.allowsToHaveFakeOverrideIn(klass) -> {
                // Trivial fake override case
                val fakeOverrideSymbol = createFakeOverrideSymbol(originalDeclaration, baseSymbol)
                classifierStorage.preCacheTypeParameters(originalDeclaration)
                fakeOverrideSymbol.fir
            }
            else -> {
                return
            }
        }
        val irDeclaration = cachedIrDeclaration(baseDeclaration) {
            // Sometimes we can have clashing here when FIR substitution/intersection override
            // have the same signature.
            // Now we avoid this problem by signature caching,
            // so both FIR overrides correspond to one IR fake override
            signatureComposer.composeSignature(baseDeclaration)
        }?.takeIf { it.parent == irClass }
            ?: createIrDeclaration(
                baseDeclaration,
                irClass,
                /* thisReceiverOwner = */ declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                origin,
                isLocal
            )
        if (containsErrorTypes(irDeclaration)) {
            return
        }
        baseSymbols[irDeclaration] = computeBaseSymbols(originalSymbol, baseSymbol, computeDirectOverridden, scope, classLookupTag)
        result += irDeclaration
    }

    private inline fun <reified S : FirCallableSymbol<*>> computeBaseSymbols(
        symbol: S,
        basedSymbol: S,
        directOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirTypeScope,
        containingClass: ConeClassLikeLookupTag,
    ): List<S> {
        if (symbol.fir.origin != FirDeclarationOrigin.IntersectionOverride) return listOf(basedSymbol)
        return scope.directOverridden(symbol).map {
            @Suppress("UNCHECKED_CAST")
            if (it.fir.isSubstitutionOverride && it.dispatchReceiverClassOrNull() == containingClass)
                it.originalForSubstitutionOverride!!
            else
                it
        }
    }

    internal fun getOverriddenSymbols(function: IrSimpleFunction): List<IrSimpleFunctionSymbol>? {
        return baseFunctionSymbols[function]?.map { declarationStorage.getIrFunctionSymbol(it) as IrSimpleFunctionSymbol }
    }

    fun bindOverriddenSymbols(declarations: List<IrDeclaration>) {
        for (declaration in declarations) {
            if (declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    val baseSymbols = getOverriddenSymbols(declaration)!!
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
            val unwrappedSymbol = baseSymbol.unwrapFakeOverrides()
            val unwrappedProperty = unwrappedSymbol.fir
            // Do not create fake overrides for accessors if not allowed to do so, e.g., private lateinit var.
            if (!(unwrappedProperty.getter?.allowsToHaveFakeOverride ?: unwrappedProperty.allowsToHaveFakeOverride)) {
                getter = null
            }
            // or private setter
            if (!(unwrappedProperty.setter?.allowsToHaveFakeOverride ?: unwrappedProperty.allowsToHaveFakeOverride)) {
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

