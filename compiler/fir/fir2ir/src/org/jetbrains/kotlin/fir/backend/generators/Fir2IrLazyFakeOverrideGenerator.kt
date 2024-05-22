/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.lazy.*
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * This class should be used ONLY for generation of fake-overrides for Fir2IrLazy... declarations
 */
class Fir2IrLazyFakeOverrideGenerator(private val c: Fir2IrComponents) : Fir2IrComponents by c {
    /**
     * For given [originalFunction] calculates original symbols for supertypes of given [klass], which contain this function
     *   in the override hierarchy.
     * This is needed to correctly build f/o keys during building the overridden functions list of the lazy function.
     *
     * For detailed examples see the documentation to [computeFakeOverrideKeysImpl]
     */
    internal fun computeFakeOverrideKeys(
        klass: FirClass,
        originalFunction: FirNamedFunctionSymbol,
    ): List<Pair<FirNamedFunctionSymbol, ConeClassLikeLookupTag>> {
        return computeFakeOverrideKeysImpl(
            klass,
            originalFunction,
            FirTypeScope::getDirectOverriddenFunctions,
            FirTypeScope::processOverriddenFunctions
        )
    }

    /**
     * For given [originalProperty] calculates original symbols for supertypes of given [klass], which contain this property
     *   in the override hierarchy.
     * This is needed to correctly build f/o keys during building the overridden functions list of the lazy property.
     *
     * For detailed examples see the documentation to [computeFakeOverrideKeysImpl]
     */
    internal fun computeFakeOverrideKeys(
        klass: FirClass,
        originalProperty: FirPropertySymbol,
    ): List<Pair<FirPropertySymbol, ConeClassLikeLookupTag>> {
        return computeFakeOverrideKeysImpl(
            klass,
            originalProperty,
            FirTypeScope::getDirectOverriddenProperties,
            FirTypeScope::processOverriddenProperties
        )
    }

    /**
     * For given [originalField] calculates original symbols for supertypes of given [klass], which contain this field
     *   in the override hierarchy.
     * This is needed to correctly build f/o keys during building the overridden functions list of the lazy field.
     *
     * For detailed examples see the documentation to [computeFakeOverrideKeysImpl]
     */
    internal fun computeFakeOverrideKeys(
        klass: FirClass,
        originalField: FirFieldSymbol,
    ): List<Pair<FirFieldSymbol, ConeClassLikeLookupTag>> {
        return computeFakeOverrideKeysImpl(
            klass,
            originalField,
            directOverridden = { listOfNotNull(originalField.originalIfFakeOverride()) },
            processOverridden = { _, _ -> shouldNotBeCalled() }
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
    private inline fun <reified S : FirCallableSymbol<*>> computeFakeOverrideKeysImpl(
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
        if (origin == FirDeclarationOrigin.RenamedForOverride) fir.initialSignatureAttr as? S ?: this else this

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

    // --------------------------------------------------------------------------------------------------------------

    /**
     * Creates dummy "substitution-override" FIR function in case when there is no actual substitution in inheritor
     * This is needed for creation of [Fir2IrLazySimpleFunction] for this fake-override, as each [Fir2IrLazySimpleFunction] should be based
     *   on the unique FIR declaration
     */
    internal fun createFirFunctionFakeOverrideIfNeeded(
        originalFunction: FirSimpleFunction,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
    ): FirSimpleFunction? {
        val originalSymbol = originalFunction.symbol
        return createFirFakeOverrideIfNeeded(
            dispatchReceiverLookupTag, originalSymbol
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

    /**
     * Creates dummy "substitution-override" FIR property in case when there is no actual substitution in inheritor
     * This is needed for creation of [Fir2IrLazyProperty] for this fake-override, as each [Fir2IrLazyProperty] should be based
     *   on the unique FIR declaration
     */
    internal fun createFirPropertyFakeOverrideIfNeeded(
        originalProperty: FirProperty,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
    ): FirProperty? {
        val originalSymbol = originalProperty.symbol
        return createFirFakeOverrideIfNeeded(
            dispatchReceiverLookupTag, originalSymbol
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

    /**
     * Creates dummy "substitution-override" FIR field in case when there is no actual substitution in inheritor
     * This is needed for creation of [Fir2IrLazyField] for this fake-override, as each [Fir2IrLazyField] should be based
     *   on the unique FIR declaration
     */
    internal fun createFirFieldFakeOverrideIfNeeded(
        originalField: FirField,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
    ): FirField? {
        val originalSymbol = originalField.symbol
        return createFirFakeOverrideIfNeeded(
            dispatchReceiverLookupTag, originalSymbol
        ) { firField ->
            val containingClass = dispatchReceiverLookupTag.toFirRegularClass(session)!!
            FirFakeOverrideGenerator.createSubstitutionOverrideField(
                session,
                firField,
                derivedClassLookupTag = dispatchReceiverLookupTag,
                newDispatchReceiverType = containingClass.defaultType(),
                origin = FirDeclarationOrigin.SubstitutionOverride.DeclarationSite,
            )
        }
    }

    private inline fun <D : FirCallableDeclaration, reified S : FirCallableSymbol<D>> createFirFakeOverrideIfNeeded(
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
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
                fakeOverrideSymbol.fir
            }

            else -> null
        }
    }
}
