/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.backend.utils.processOverriddenFunctionSymbols
import org.jetbrains.kotlin.fir.backend.utils.processOverriddenPropertySymbols
import org.jetbrains.kotlin.fir.backend.utils.unwrapSubstitutionAndIntersectionOverrides
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This api is planned for deprecation. Make sure, you don't use it with useIrFakeOverrideBuilder=true and non-lazy classes"
)
annotation class FirBasedFakeOverrideGenerator

@FirBasedFakeOverrideGenerator
class FakeOverrideGenerator(
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by c {
    private val baseFunctionSymbols: MutableMap<IrSimpleFunctionSymbol, List<FirNamedFunctionSymbol>> = mutableMapOf()
    private val basePropertySymbols: MutableMap<IrPropertySymbol, List<FirPropertySymbol>> = mutableMapOf()
    private val baseStaticFieldSymbols: MutableMap<IrFieldSymbol, List<FirFieldSymbol>> = mutableMapOf()

    private inline fun IrSimpleFunction.withFunction(f: IrSimpleFunction.() -> Unit) {
        conversionScope.withFunction(this, f)
    }

    private inline fun IrProperty.withProperty(f: IrProperty.() -> Unit) {
        conversionScope.withProperty(this, firProperty = null, f)
    }

    private fun IrType.containsErrorType(): Boolean {
        return when (this) {
            is IrErrorType -> true
            is IrSimpleType -> arguments.any { it is IrTypeProjection && it.type.containsErrorType() }
            else -> false
        }
    }

    fun computeFakeOverrides(firClass: FirClass, irClass: IrClass, realDeclarations: Collection<FirDeclaration>) {
        val result = mutableListOf<IrDeclaration>()
        val useSiteMemberScope = firClass.unsubstitutedScope(c)

        val superTypesCallableNames = useSiteMemberScope.getCallableNames()
        val realDeclarationSymbols = realDeclarations.mapTo(mutableSetOf(), FirDeclaration::symbol)

        for (name in superTypesCallableNames) {
            generateFakeOverridesForName(irClass, useSiteMemberScope, name, firClass, result, realDeclarationSymbols)
        }
    }

    fun generateFakeOverridesForName(
        irClass: IrClass,
        name: Name,
        firClass: FirClass
    ): List<IrDeclaration> = buildList {
        val useSiteMemberScope = firClass.unsubstitutedScope(c)

        generateFakeOverridesForName(
            irClass, useSiteMemberScope, name, firClass, this,
            // This parameter is only needed for data-class methods that is irrelevant for lazy library classes
            realDeclarationSymbols = emptySet()
        )
        val staticScope = firClass.scopeProvider.getStaticMemberScopeForCallables(firClass, session, scopeSession)
        if (staticScope != null) {
            generateFakeOverridesForName(
                irClass, staticScope, name, firClass, this,
                // This parameter is only needed for data-class methods that is irrelevant for lazy library classes
                realDeclarationSymbols = emptySet()
            )
        }
    }

    private fun generateFakeOverridesForName(
        irClass: IrClass,
        useSiteOrStaticScope: FirScope,
        name: Name,
        firClass: FirClass,
        result: MutableList<IrDeclaration>?,
        realDeclarationSymbols: Set<FirBasedSymbol<*>>
    ) {
        val isLocal = firClass !is FirRegularClass || firClass.isLocal
        useSiteOrStaticScope.processFunctionsByName(name) { functionSymbol ->
            createFakeOverriddenIfNeeded(
                firClass, irClass, isLocal, functionSymbol,
                declarationStorage::getCachedIrFunctionSymbol,
                { function, irParent, isLocal ->
                    declarationStorage.createAndCacheIrFunction(
                        function,
                        irParent,
                        IrDeclarationOrigin.FAKE_OVERRIDE,
                        isLocal,
                        allowLazyDeclarationsCreation = true
                    )
                },
                createFakeOverrideSymbol = { firFunction, callableSymbol ->
                    val symbol = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(callableSymbol, firClass.symbol.classId)
                    FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
                        session, symbol, firFunction,
                        derivedClassLookupTag = firClass.symbol.toLookupTag(),
                        newDispatchReceiverType = firClass.defaultType(),
                        origin = FirDeclarationOrigin.SubstitutionOverride.DeclarationSite,
                        isExpect = (firClass as? FirRegularClass)?.isExpect == true || firFunction.isExpect
                    )
                },
                baseFunctionSymbols,
                result,
                containsErrorTypes = { irFunction ->
                    irFunction.returnType.containsErrorType() || irFunction.valueParameters.any { it.type.containsErrorType() }
                },
                realDeclarationSymbols,
                FirTypeScope::getDirectOverriddenFunctions,
                useSiteOrStaticScope,
            )
        }

        useSiteOrStaticScope.processPropertiesByName(name) { propertyOrFieldSymbol ->
            when (propertyOrFieldSymbol) {
                is FirPropertySymbol -> {
                    createFakeOverriddenIfNeeded(
                        firClass, irClass, isLocal, propertyOrFieldSymbol,
                        declarationStorage::getCachedIrPropertySymbol,
                        { property, irParent, _ ->
                            declarationStorage.createAndCacheIrProperty(
                                property,
                                irParent,
                                IrDeclarationOrigin.FAKE_OVERRIDE,
                                allowLazyDeclarationsCreation = true
                            )
                        },
                        createFakeOverrideSymbol = { firProperty, callableSymbol ->
                            val symbolForOverride =
                                FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(callableSymbol, firClass.symbol.classId)
                            FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
                                session, symbolForOverride, firProperty,
                                derivedClassLookupTag = firClass.symbol.toLookupTag(),
                                newDispatchReceiverType = firClass.defaultType(),
                                origin = FirDeclarationOrigin.SubstitutionOverride.DeclarationSite,
                                isExpect = (firClass as? FirRegularClass)?.isExpect == true || firProperty.isExpect
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
                        useSiteOrStaticScope,
                    )
                }

                is FirFieldSymbol -> {
                    if (!propertyOrFieldSymbol.isStatic) return@processPropertiesByName
                    createFakeOverriddenIfNeeded(
                        firClass, irClass, isLocal, propertyOrFieldSymbol,
                        { field, _, _ -> declarationStorage.getCachedIrFieldStaticFakeOverrideSymbolByDeclaration(field) },
                        { field, irParent, _ -> declarationStorage.getOrCreateIrField(field, irParent) },
                        createFakeOverrideSymbol = { firField, _ ->
                            FirFakeOverrideGenerator.createSubstitutionOverrideField(
                                session, firField,
                                derivedClassLookupTag = firClass.symbol.toLookupTag(),
                                newReturnType = firField.returnTypeRef.coneType,
                                origin = FirDeclarationOrigin.SubstitutionOverride.DeclarationSite,
                            )
                        },
                        baseStaticFieldSymbols,
                        result,
                        containsErrorTypes = { irField -> irField.type.containsErrorType() },
                        realDeclarationSymbols,
                        computeDirectOverridden = { emptyList() },
                        useSiteOrStaticScope,
                    )
                }

                else -> {
                }
            }
        }
    }

    internal fun calcBaseSymbolsForFakeOverrideFunction(
        klass: FirClass,
        fakeOverride: IrSimpleFunction,
        originalSymbol: FirNamedFunctionSymbol,
    ) {
        val scope = klass.unsubstitutedScope(c)
        val classLookupTag = klass.symbol.toLookupTag()
        val baseFirSymbolsForFakeOverride =
            if (originalSymbol.shouldHaveComputedBaseSymbolsForClass(classLookupTag)) {
                computeBaseSymbols(originalSymbol, FirTypeScope::getDirectOverriddenFunctions, scope, classLookupTag)
            } else {
                listOf(originalSymbol)
            }
        baseFunctionSymbols[fakeOverride.symbol] = baseFirSymbolsForFakeOverride
    }

    internal fun computeBaseSymbolsWithContainingClass(
        klass: FirClass,
        originalFunction: FirNamedFunctionSymbol,
    ): List<Pair<FirNamedFunctionSymbol, ConeClassLikeLookupTag>> {
        return computeBaseSymbolsWithContainingClass(
            klass,
            originalFunction,
            FirTypeScope::getDirectOverriddenFunctions,
            FirTypeScope::processOverriddenFunctions
        )
    }

    internal fun computeBaseSymbolsWithContainingClass(
        klass: FirClass,
        originalFunction: FirPropertySymbol,
    ): List<Pair<FirPropertySymbol, ConeClassLikeLookupTag>> {
        return computeBaseSymbolsWithContainingClass(
            klass,
            originalFunction,
            FirTypeScope::getDirectOverriddenProperties,
            FirTypeScope::processOverriddenProperties
        )
    }

    /**
     * This functions takes a list of overridden symbols and associate each one with one of the supertypes, from which this symbol came
     *   from. This mapping will be used later to generate proper fake-overrides in IR
     *
     * ```
     * open class A {
     *     fun foo() {}
     * }
     *
     * interface B
     *
     * open class C : A()
     *
     * class D : C(), B {
     *     override fun foo() {}
     * }
     * ```
     *
     * In this example FIR returns that `C.foo` overrides `A.foo`
     * But in IR there are fake-overrides on each level of the hierarchy, so we need to associate `A.foo` with class `C`,
     *   so later it will be converted to IR f/o `B.foo`
     * To understand that we should choose `C` here instead of `B`, we check if the dispatch receiver of the overridden is supertype
     *   of each of supertypes (here `A` from `A.foo` is not supertype of `B`, so supertype `B` is discarded)
     *
     * There might be some cases when the same overridden function is accessible from multiple supertypes.
     * Note that this example is simplified, and actually requires java classes on the way (see KT-65592)
     *
     * ```
     * interface A {
     *     fun foo()
     * }
     *
     * open class AImpl : A {
     *     override fun foo() {}
     * }
     *
     * interface B : A
     * open class BImpl : AImpl(), B
     *
     * interface C : B
     * class CImpl : BImpl(), C
     * ```
     *
     * Here we have f/o `CImpl.foo` function, which overrides `A.foo` and `AImpl.foo` from FIR point of view
     * `AImpl.foo` matches only with supertype `BImpl`
     * But receiver of `A.foo` is a supertype both of `BImpl` and `C`, which leads to the situation, when there are two different base
     *   overridden functions matches `BImpl` supertype
     * To resolve this ambiguity (which one choose, `A.foo` or `AImpl.foo`), we need to understand, which of those functions override
     *   all other candidates (see [chooseMostSpecificOverridden] function) and leave only `AImpl.foo`
     */
    private inline fun <reified S : FirCallableSymbol<*>> computeBaseSymbolsWithContainingClass(
        klass: FirClass,
        originalSymbol: S,
        directOverridden: FirTypeScope.(S) -> List<S>,
        processOverridden: FirTypeScope.(S, (S) -> ProcessorAction) -> ProcessorAction
    ): List<Pair<S, ConeClassLikeLookupTag>> {
        val scope = klass.unsubstitutedScope(c)
        val classLookupTag = klass.symbol.toLookupTag()
        val overriddenFirSymbols = computeBaseSymbols(originalSymbol, directOverridden, scope, classLookupTag)
        val typeContext = session.typeContext
        val overriddenPerSupertype = setMultimapOf<ConeClassLikeLookupTag, S>()
        with(typeContext) {
            for (symbol in overriddenFirSymbols) {
                val symbolDispatchReceiver = symbol.containingClassLookupTag() ?: continue
                for (superType in klass.superConeTypes) {
                    val compatibleType = superType.anySuperTypeConstructor {
                        it.typeConstructor() == symbolDispatchReceiver
                    }
                    if (!compatibleType) {
                        continue
                    }
                    overriddenPerSupertype.put(superType.lookupTag, symbol)
                }
            }
        }

        val result = overriddenPerSupertype.map { (superType, overridden) ->
            val chosenOverridden = when (overridden.size) {
                0 -> shouldNotBeCalled()
                1 -> overridden.first()
                else -> chooseMostSpecificOverridden(superType, overridden, processOverridden)
            }
            chosenOverridden to superType
        }

        return result
    }

    private inline fun <reified S : FirCallableSymbol<*>> chooseMostSpecificOverridden(
        containingClassLookupTag: ConeClassLikeLookupTag,
        overridden: Collection<S>,
        processOverridden: FirTypeScope.(S, (S) -> ProcessorAction) -> ProcessorAction
    ): S {
        val scope = containingClassLookupTag.toFirRegularClassSymbol(session)?.unsubstitutedScope(c) ?: return overridden.first()

        val result = overridden.firstOrNull { s1 ->
            overridden.all { s2 ->
                if (s1 == s2) return@all true
                scope.anyOverriddenOf(s1, processOverridden) { it == s2 }
            }
        }

        return result ?: overridden.first()
    }

    internal fun calcBaseSymbolsForFakeOverrideProperty(
        klass: FirClass,
        fakeOverride: IrProperty,
        originalSymbol: FirPropertySymbol,
    ) {
        val scope = klass.unsubstitutedScope(c)
        val classLookupTag = klass.symbol.toLookupTag()
        val baseFirSymbolsForFakeOverride =
            if (originalSymbol.shouldHaveComputedBaseSymbolsForClass(classLookupTag)) {
                computeBaseSymbols(originalSymbol, FirTypeScope::getDirectOverriddenProperties, scope, classLookupTag)
            } else {
                listOf(originalSymbol)
            }
        basePropertySymbols[fakeOverride.symbol] = baseFirSymbolsForFakeOverride
    }

    private fun FirCallableSymbol<*>.shouldHaveComputedBaseSymbolsForClass(classLookupTag: ConeClassLikeLookupTag): Boolean =
        fir.origin.fromSupertypes && classLookupTag.isRealOwnerOf(this)

    @Suppress("IncorrectFormatting")
    private inline fun <
        reified D : FirCallableDeclaration,
        reified S : FirCallableSymbol<D>,
        reified ID : IrDeclaration,
        reified IS : IrBindableSymbol<*, ID>
    > createFakeOverriddenIfNeeded(
        klass: FirClass,
        irClass: IrClass,
        isLocal: Boolean,
        originalSymbol: FirCallableSymbol<*>,
        cachedIrDeclarationSymbol: (firDeclaration: D, dispatchReceiverLookupTag: ConeClassLikeLookupTag?, () -> IdSignature?) -> IS?,
        createIrDeclaration: (firDeclaration: D, irParent: IrClass, isLocal: Boolean) -> ID,
        createFakeOverrideSymbol: (firDeclaration: D, baseSymbol: S) -> S,
        baseSymbols: MutableMap<IS, List<S>>,
        result: MutableList<in ID>?,
        containsErrorTypes: (ID) -> Boolean,
        realDeclarationSymbols: Set<FirBasedSymbol<*>>,
        computeDirectOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirScope,
    ) {
        if (originalSymbol !is S) return
        val classLookupTag = klass.symbol.toLookupTag()
        val originalDeclaration = originalSymbol.fir

        if (originalSymbol.containingClassLookupTag() == classLookupTag && !originalDeclaration.origin.fromSupertypes) return
        // Data classes' methods from Any (toString/equals/hashCode) are not handled by the line above because they have Any-typed dispatch receiver
        // (there are no special FIR method for them, it's just fake overrides)
        // But they are treated differently in IR (real declarations have already been declared before) and such methods are present among realDeclarationSymbols
        if (originalSymbol in realDeclarationSymbols) return

        val baseSymbol = originalSymbol.unwrapSubstitutionAndIntersectionOverrides() as S

        if (!session.visibilityChecker.isVisibleForOverriding(klass.moduleData, klass.symbol, baseSymbol.fir)) return
        if (originalDeclaration.originalOrSelf().origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk) return

        val (fakeOverrideFirDeclaration, baseFirSymbolsForFakeOverride) = when {
            originalSymbol.shouldHaveComputedBaseSymbolsForClass(classLookupTag) -> {
                // Substitution or intersection case
                // We have already a FIR declaration for such fake override
                originalDeclaration to computeBaseSymbols(originalSymbol, computeDirectOverridden, scope, classLookupTag)
            }
            originalDeclaration.allowsToHaveFakeOverride -> {
                // Trivial fake override case
                // We've got no relevant declaration in FIR world for such a fake override in current class, thus we're creating it here
                val fakeOverrideSymbol = createFakeOverrideSymbol(originalDeclaration, baseSymbol)
                declarationStorage.saveFakeOverrideInClass(irClass, originalDeclaration, fakeOverrideSymbol.fir)
                fakeOverrideSymbol.fir to listOf(originalSymbol)
            }
            else -> {
                return
            }
        }
        val irSymbol = cachedIrDeclarationSymbol(fakeOverrideFirDeclaration, null) {
            // Sometimes we can have clashing here when FIR substitution/intersection override
            // have the same signature.
            // Now we avoid this problem by signature caching,
            // so both FIR overrides correspond to one IR fake override
            signatureComposer.composeSignature(fakeOverrideFirDeclaration)
        }?.takeIf {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            it.ownerIfBound()?.parent == irClass
        } ?: createIrDeclaration(fakeOverrideFirDeclaration, irClass, isLocal).symbol as IS

        baseSymbols[irSymbol] = baseFirSymbolsForFakeOverride

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val owner = irSymbol.owner
        if (containsErrorTypes(owner)) {
            return
        }

        result?.add(owner)
    }

    private inline fun <D : FirCallableDeclaration, reified S : FirCallableSymbol<D>> createFirFakeOverrideIfNeeded(
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
        irClass: IrClass,
        originalSymbol: S,
        createFakeOverrideSymbol: (firDeclaration: D) -> S,
    ): D? {
        val originalDeclaration = originalSymbol.fir
        return when {
            originalSymbol.containingClassLookupTag() == dispatchReceiverLookupTag -> null

            originalDeclaration.allowsToHaveFakeOverride -> {
                // Trivial fake override case
                // We've got no relevant declaration in FIR world for such a fake override in current class, thus we're creating it here
                val fakeOverrideSymbol = createFakeOverrideSymbol(originalDeclaration)
                declarationStorage.saveFakeOverrideInClass(irClass, originalDeclaration, fakeOverrideSymbol.fir)
                fakeOverrideSymbol.fir
            }

            else -> null
        }
    }

    internal fun createFirFunctionFakeOverrideIfNeeded(
        originalFunction: FirSimpleFunction,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
        irClass: IrClass,
    ): FirSimpleFunction? {
        val originalSymbol = originalFunction.symbol
        return createFirFakeOverrideIfNeeded(
            dispatchReceiverLookupTag, irClass, originalSymbol
        ) { firFunction ->
            val containingClass = dispatchReceiverLookupTag.toFirRegularClass(session)!!
            FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
                session,
                FirNamedFunctionSymbol(CallableId(containingClass.symbol.classId, originalSymbol.callableId.callableName)),
                firFunction,
                derivedClassLookupTag = dispatchReceiverLookupTag,
                newDispatchReceiverType = containingClass.defaultType(),
                origin = FirDeclarationOrigin.SubstitutionOverride.DeclarationSite,
                isExpect = containingClass.isExpect || firFunction.isExpect
            )
        }
    }

    internal fun createFirPropertyFakeOverrideIfNeeded(
        originalProperty: FirProperty,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
        irClass: IrClass,
    ): FirProperty? {
        val originalSymbol = originalProperty.symbol
        return createFirFakeOverrideIfNeeded(
            dispatchReceiverLookupTag, irClass, originalSymbol
        ) { firProperty ->
            val containingClass = dispatchReceiverLookupTag.toFirRegularClass(session)!!
            FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
                session,
                FirPropertySymbol(CallableId(containingClass.symbol.classId, originalSymbol.callableId.callableName)),
                firProperty,
                derivedClassLookupTag = dispatchReceiverLookupTag,
                newDispatchReceiverType = containingClass.defaultType(),
                origin = FirDeclarationOrigin.SubstitutionOverride.DeclarationSite,
                isExpect = containingClass.isExpect || firProperty.isExpect
            )
        }
    }

    private inline fun <reified S : FirCallableSymbol<*>> computeBaseSymbols(
        symbol: S,
        directOverridden: FirTypeScope.(S) -> List<S>,
        scope: FirScope,
        containingClass: ConeClassLikeLookupTag,
    ): List<S> {
        if (symbol.fir.origin is FirDeclarationOrigin.SubstitutionOverride) {
            return listOf(symbol.originalForSubstitutionOverride!!)
        }

        if (scope !is FirTypeScope) {
            return emptyList()
        }

        return scope.directOverridden(symbol).map {
            it.unwrapRenamedForOverride().unwrapSubstitutionOverride(containingClass)
        }
    }

    private inline fun <reified S : FirCallableSymbol<*>> S.unwrapSubstitutionOverride(containingClass: ConeClassLikeLookupTag): S =
        // Unwrapping should happen only for fake overrides members from the same class, not from supertypes
        if (containingClass.isRealOwnerOf(this) && fir.isSubstitutionOverride) originalForSubstitutionOverride!! else this

    private inline fun <reified S : FirCallableSymbol<*>> S.unwrapRenamedForOverride(): S =
        if (origin == FirDeclarationOrigin.RenamedForOverride) fir.initialSignatureAttr?.symbol as? S ?: this else this

    internal fun getOverriddenSymbolsForFakeOverride(function: IrSimpleFunction): List<IrSimpleFunctionSymbol>? {
        val baseSymbols = baseFunctionSymbols[function.symbol] ?: return null
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

    internal fun getOverriddenSymbolsForFakeOverride(property: IrProperty): List<IrPropertySymbol>? {
        val baseSymbols = basePropertySymbols[property.symbol] ?: return null
        return getOverriddenSymbolsInSupertypes(
            property,
            baseSymbols
        ) { declarationStorage.getIrPropertySymbol(it) as IrPropertySymbol }
    }

    private fun <I : IrDeclaration, S : IrSymbol, F : FirCallableSymbol<*>> getOverriddenSymbolsInSupertypes(
        declaration: I,
        baseSymbols: List<F>,
        irProducer: (F) -> S,
    ): List<S> {
        val irClass = declaration.parentAsClass

        @OptIn(UnsafeDuringIrConstructionAPI::class)
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
            overridden.containingClassLookupTag()?.toSymbol(session)?.fir as? FirClass ?: return emptyList()

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val overriddenContainingIrClass =
            declarationStorage.classifierStorage.getIrClassSymbol(overriddenContainingClass.symbol).owner

        return superClasses.mapNotNull { superClass ->
            if (superClass == overriddenContainingIrClass ||
                // Note: Kotlin static scopes cannot find base symbol in this case, so we have to fallback to the very base declaration
                overridden.isStatic && superClass.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            ) {
                // `overridden` was a FIR declaration in some supertypes
                irProducer(overridden)
            } else {
                // There were no FIR declaration in supertypes, but we know that we have fake overrides in some of them
                declarationStorage.getFakeOverrideInClass(superClass, overridden.fir)?.let { fakeOverrideInClass ->
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
                    val baseSymbols = basePropertySymbols[declaration.symbol]!!
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

        getter?.apply {
            overriddenSymbols = overriddenIrSymbols.mapNotNull { declarationStorage.findGetterOfProperty(it) }
        }
        if (isVar) {
            setter?.apply {
                overriddenSymbols = overriddenIrSymbols.mapNotNull { declarationStorage.findSetterOfProperty(it) }
            }
        }
        overriddenSymbols = overriddenIrSymbols
        return this
    }
}

@FirBasedFakeOverrideGenerator
internal fun FirProperty.generateOverriddenAccessorSymbols(
    containingClass: FirClass,
    isGetter: Boolean,
    c: Fir2IrComponents,
): List<IrSimpleFunctionSymbol> {
    val scope = containingClass.unsubstitutedScope(c)
    scope.processPropertiesByName(name) {}
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()
    val superClasses = containingClass.getSuperTypesAsIrClasses(c)

    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!c.session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }

        for (overriddenIrPropertySymbol in c.fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(overriddenSymbol, superClasses)) {
            val overriddenIrAccessorSymbol =
                if (isGetter) c.declarationStorage.findGetterOfProperty(overriddenIrPropertySymbol)
                else c.declarationStorage.findSetterOfProperty(overriddenIrPropertySymbol)
            if (overriddenIrAccessorSymbol != null) {
                assert(overriddenIrAccessorSymbol != symbol) { "Cannot add property $overriddenIrAccessorSymbol to its own overriddenSymbols" }
                overriddenSet += overriddenIrAccessorSymbol
            }
        }
        ProcessorAction.NEXT
    }
    return overriddenSet.toList()
}

@FirBasedFakeOverrideGenerator
internal fun FirProperty.generateOverriddenPropertySymbols(containingClass: FirClass, c: Fir2IrComponents): List<IrPropertySymbol> {
    val superClasses = containingClass.getSuperTypesAsIrClasses(c)
    val overriddenSet = mutableSetOf<IrPropertySymbol>()

    processOverriddenPropertySymbols(containingClass, c) {
        for (overridden in c.fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            assert(overridden != symbol) { "Cannot add property $overridden to its own overriddenSymbols" }
            overriddenSet += overridden
        }
    }

    return overriddenSet.toList()
}

@FirBasedFakeOverrideGenerator
internal fun FirSimpleFunction.generateOverriddenFunctionSymbols(
    containingClass: FirClass,
    c: Fir2IrComponents,
): List<IrSimpleFunctionSymbol> {
    val superClasses = containingClass.getSuperTypesAsIrClasses(c)
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()

    processOverriddenFunctionSymbols(containingClass, c) {
        for (overridden in c.fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            assert(overridden != symbol) { "Cannot add function $overridden to its own overriddenSymbols" }
            overriddenSet += overridden
        }
    }

    return overriddenSet.toList()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun FirClass.getSuperTypesAsIrClasses(c: Fir2IrComponents): Set<IrClass> {
    val irClass = c.declarationStorage.classifierStorage.getIrClassSymbol(symbol).owner
    return irClass.superTypes.mapNotNull { it.classifierOrNull?.owner as? IrClass }.toSet()
}
