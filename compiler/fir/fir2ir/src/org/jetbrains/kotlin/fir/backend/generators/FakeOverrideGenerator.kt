/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrDeclarationStorage
import org.jetbrains.kotlin.fir.backend.unwrapSubstitutionAndIntersectionOverrides
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.allowsToHaveFakeOverride
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name

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
        return conversionScope.withProperty(this, firProperty = null, f)
    }

    private fun FirCallableDeclaration.allowsToHaveFakeOverrideIn(klass: FirClass): Boolean {
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

    fun IrClass.addFakeOverrides(klass: FirClass, declarations: Collection<FirDeclaration>) {
        this.declarations += getFakeOverrides(
            klass,
            declarations
        )
    }

    private fun IrClass.getFakeOverrides(klass: FirClass, realDeclarations: Collection<FirDeclaration>): List<IrDeclaration> {
        val result = mutableListOf<IrDeclaration>()
        val useSiteMemberScope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        val superTypesCallableNames = useSiteMemberScope.getCallableNames()
        val realDeclarationSymbols = realDeclarations.mapTo(mutableSetOf(), FirDeclaration::symbol)

        for (name in superTypesCallableNames) {
            generateFakeOverridesForName(this, useSiteMemberScope, name, klass, result, realDeclarationSymbols)
        }
        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun generateFakeOverridesForName(
        irClass: IrClass,
        name: Name,
        firClass: FirClass
    ): List<IrDeclaration> = buildList {
        val useSiteMemberScope = firClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        generateFakeOverridesForName(
            irClass, useSiteMemberScope, name, firClass, this,
            // This parameter is only needed for data-class methods that is irrelevant for lazy library classes
            realDeclarationSymbols = emptySet()
        )
    }

    internal fun generateFakeOverridesForName(
        irClass: IrClass,
        useSiteMemberScope: FirTypeScope,
        name: Name,
        firClass: FirClass,
        result: MutableList<IrDeclaration>,
        realDeclarationSymbols: Set<FirBasedSymbol<*>>
    ) {
        val isLocal = firClass !is FirRegularClass || firClass.isLocal
        useSiteMemberScope.processFunctionsByName(name) { functionSymbol ->
            createFakeOverriddenIfNeeded(
                firClass, irClass, isLocal, functionSymbol,
                declarationStorage::getCachedIrFunction,
                declarationStorage::createIrFunction,
                createFakeOverrideSymbol = { firFunction, callableSymbol ->
                    val symbol = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(callableSymbol, firClass.symbol.classId)
                    FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
                        session, symbol, firFunction,
                        newDispatchReceiverType = firClass.defaultType(),
                        isExpect = (firClass as? FirRegularClass)?.isExpect == true
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
                firClass, irClass, isLocal, propertySymbol,
                declarationStorage::getCachedIrProperty,
                declarationStorage::createIrProperty,
                createFakeOverrideSymbol = { firProperty, callableSymbol ->
                    val symbolForOverride = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(callableSymbol, firClass.symbol.classId)
                    FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
                        session, symbolForOverride, firProperty,
                        newDispatchReceiverType = firClass.defaultType(),
                        isExpect = (firClass as? FirRegularClass)?.isExpect == true
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

    internal fun calcBaseSymbolsForFakeOverrideFunction(
        klass: FirClass,
        fakeOverride: IrSimpleFunction,
        originalSymbol: FirNamedFunctionSymbol,
    ) {
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        val classLookupTag = klass.symbol.toLookupTag()
        val baseFirSymbolsForFakeOverride =
            if (originalSymbol.shouldHaveComputedBaseSymbolsForClass(classLookupTag)) {
                computeBaseSymbols(originalSymbol, FirTypeScope::getDirectOverriddenFunctions, scope, classLookupTag)
            } else {
                listOf(originalSymbol)
            }
        baseFunctionSymbols[fakeOverride] = baseFirSymbolsForFakeOverride
    }

    private fun FirCallableSymbol<*>.shouldHaveComputedBaseSymbolsForClass(classLookupTag: ConeClassLikeLookupTag): Boolean =
        fir.origin.fromSupertypes && dispatchReceiverClassOrNull() == classLookupTag

    private inline fun <reified D : FirCallableDeclaration, reified S : FirCallableSymbol<D>, reified I : IrDeclaration> createFakeOverriddenIfNeeded(
        klass: FirClass,
        irClass: IrClass,
        isLocal: Boolean,
        originalSymbol: FirCallableSymbol<*>,
        cachedIrDeclaration: (firDeclaration: D, dispatchReceiverLookupTag: ConeClassLikeLookupTag?, () -> IdSignature?) -> I?,
        createIrDeclaration: (firDeclaration: D, irParent: IrClass, thisReceiverOwner: IrClass?, origin: IrDeclarationOrigin, isLocal: Boolean) -> I,
        createFakeOverrideSymbol: (firDeclaration: D, baseSymbol: S) -> S,
        baseSymbols: MutableMap<I, List<S>>,
        result: MutableList<in I>,
        containsErrorTypes: (I) -> Boolean,
        realDeclarationSymbols: Set<FirBasedSymbol<*>>,
        computeDirectOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirTypeScope,
    ) {
        if (originalSymbol !is S) return
        val classLookupTag = klass.symbol.toLookupTag()
        val originalDeclaration = originalSymbol.fir

        if (originalSymbol.dispatchReceiverClassOrNull() == classLookupTag && !originalDeclaration.origin.fromSupertypes) return
        // Data classes' methods from Any (toString/equals/hashCode) are not handled by the line above because they have Any-typed dispatch receiver
        // (there are no special FIR method for them, it's just fake overrides)
        // But they are treated differently in IR (real declarations have already been declared before) and such methods are present among realDeclarationSymbols
        if (originalSymbol in realDeclarationSymbols) return

        if (originalDeclaration.visibility == Visibilities.Private) return

        val origin = IrDeclarationOrigin.FAKE_OVERRIDE
        val baseSymbol = originalSymbol.unwrapSubstitutionAndIntersectionOverrides() as S

        val (fakeOverrideFirDeclaration, baseFirSymbolsForFakeOverride) = when {
            originalSymbol.shouldHaveComputedBaseSymbolsForClass(classLookupTag) -> {
                // Substitution or intersection case
                // We have already a FIR declaration for such fake override
                originalDeclaration to computeBaseSymbols(originalSymbol, computeDirectOverridden, scope, classLookupTag)
            }
            originalDeclaration.allowsToHaveFakeOverrideIn(klass) -> {
                // Trivial fake override case
                // We've got no relevant declaration in FIR world for such a fake override in current class, thus we're creating it here
                val fakeOverrideSymbol = createFakeOverrideSymbol(originalDeclaration, baseSymbol)
                declarationStorage.saveFakeOverrideInClass(irClass, originalDeclaration, fakeOverrideSymbol.fir)
                classifierStorage.preCacheTypeParameters(originalDeclaration)
                fakeOverrideSymbol.fir to listOf(originalSymbol)
            }
            else -> {
                return
            }
        }
        val irDeclaration = cachedIrDeclaration(fakeOverrideFirDeclaration, null) {
            // Sometimes we can have clashing here when FIR substitution/intersection override
            // have the same signature.
            // Now we avoid this problem by signature caching,
            // so both FIR overrides correspond to one IR fake override
            signatureComposer.composeSignature(fakeOverrideFirDeclaration)
        }?.takeIf { it.parent == irClass }
            ?: createIrDeclaration(
                fakeOverrideFirDeclaration,
                irClass,
                /* thisReceiverOwner = */ declarationStorage.findIrParent(baseSymbol.fir) as? IrClass,
                origin,
                isLocal
            )
        if (containsErrorTypes(irDeclaration)) {
            return
        }
        baseSymbols[irDeclaration] = baseFirSymbolsForFakeOverride
        result += irDeclaration
    }

    private inline fun <reified S : FirCallableSymbol<*>> computeBaseSymbols(
        symbol: S,
        directOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirTypeScope,
        containingClass: ConeClassLikeLookupTag,
    ): List<S> {
        if (symbol.fir.origin == FirDeclarationOrigin.SubstitutionOverride) {
            return listOf(symbol.originalForSubstitutionOverride!!)
        }

        return scope.directOverridden(symbol).map {
            // Unwrapping should happen only for fake overrides members from the same class, not from supertypes
            if (it.fir.isSubstitutionOverride && it.dispatchReceiverClassOrNull() == containingClass)
                it.originalForSubstitutionOverride!!
            else
                it
        }
    }

    internal fun getOverriddenSymbolsForFakeOverride(function: IrSimpleFunction): List<IrSimpleFunctionSymbol>? {
        val baseSymbols = baseFunctionSymbols[function] ?: return null
        return getOverriddenSymbolsInSupertypes(
            function,
            baseSymbols
        ) { declarationStorage.getIrFunctionSymbol(it) as IrSimpleFunctionSymbol }
    }

    internal fun getOverriddenSymbolsInSupertypes(
        overridden: FirNamedFunctionSymbol,
        superClasses: Set<IrClass>,
    ): List<IrSimpleFunctionSymbol> {
        return getOverriddenSymbolsInSupertypes(
            overridden,
            superClasses
        ) { declarationStorage.getIrFunctionSymbol(it) as IrSimpleFunctionSymbol }
    }

    internal fun getOverriddenSymbolsInSupertypes(
        overridden: FirPropertySymbol,
        superClasses: Set<IrClass>,
    ): List<IrPropertySymbol> {
        return getOverriddenSymbolsInSupertypes(
            overridden, superClasses
        ) { declarationStorage.getIrPropertySymbol(it) as IrPropertySymbol }
    }

    private fun <I : IrDeclaration, S : IrSymbol, F : FirCallableSymbol<*>> getOverriddenSymbolsInSupertypes(
        declaration: I,
        baseSymbols: List<F>,
        irProducer: (F) -> S,
    ): List<S> {
        val irClass = declaration.parentAsClass
        val superClasses = irClass.superTypes.mapNotNull { it.classifierOrNull?.owner as? IrClass }.toSet()

        return baseSymbols.flatMap { overridden ->
            getOverriddenSymbolsInSupertypes(overridden, superClasses, irProducer)
        }.distinct()
    }

    private fun <F : FirCallableSymbol<*>, S : IrSymbol> getOverriddenSymbolsInSupertypes(
        overridden: F,
        superClasses: Set<IrClass>,
        irProducer: (F) -> S
    ): List<S> {
        val overriddenContainingClass =
            overridden.containingClass()?.toSymbol(session)?.fir as? FirClass ?: return emptyList()

        val overriddenContainingIrClass =
            declarationStorage.classifierStorage.getIrClassSymbol(overriddenContainingClass.symbol).owner as? IrClass
                ?: return emptyList()

        return if (overriddenContainingIrClass in superClasses) {
            // `overridden` was a FIR declaration in some of the supertypes
            listOf(irProducer(overridden))
        } else {
            // There were no FIR declaration in supertypes, but we know that we have fake overrides in some of them
            superClasses.mapNotNull {
                declarationStorage.getFakeOverrideInClass(it, overridden.fir)?.let { fakeOverrideInClass ->
                    @Suppress("UNCHECKED_CAST")
                    irProducer(fakeOverrideInClass.symbol as F)
                }
            }
        }
    }

    fun bindOverriddenSymbols(declarations: List<IrDeclaration>) {
        for (declaration in declarations) {
            if (declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    val baseSymbols = getOverriddenSymbolsForFakeOverride(declaration)!!
                    declaration.withFunction {
                        overriddenSymbols = baseSymbols
                    }
                }
                is IrProperty -> {
                    val baseSymbols = basePropertySymbols[declaration]!!
                    declaration.withProperty {
                        discardAccessorsAccordingToBaseVisibility(baseSymbols)
                        setOverriddenSymbolsForProperty(declarationStorage, declaration.isVar, baseSymbols)
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

    private fun IrProperty.setOverriddenSymbolsForProperty(
        declarationStorage: Fir2IrDeclarationStorage,
        isVar: Boolean,
        firOverriddenSymbols: List<FirPropertySymbol>
    ): IrProperty {
        val overriddenIrSymbols = getOverriddenSymbolsInSupertypes(this, firOverriddenSymbols) {
            declarationStorage.getIrPropertySymbol(it) as IrPropertySymbol
        }
        val overriddenIrProperties = overriddenIrSymbols.map { it.owner }

        getter?.apply {
            overriddenSymbols = overriddenIrProperties.mapNotNull { it.getter?.symbol }
        }
        if (isVar) {
            setter?.apply {
                overriddenSymbols = overriddenIrProperties.mapNotNull { it.setter?.symbol }
            }
        }
        overriddenSymbols = overriddenIrSymbols
        return this
    }
}

