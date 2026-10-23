/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirConstructorSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.findSyntheticJavaPropertyAccessor
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.getClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.fir.toKaImplementationState
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOverloadabilityHelper.ContextParameterShadowing.BothWays
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.analysis.api.fir.components.isDirectSubClassOf as computeIsDirectSubClassOf
import org.jetbrains.kotlin.analysis.api.fir.components.isSubClassOf as computeIsSubClassOf

internal class KaFirSymbolRelationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaInternalsSymbolRelationProvider {
    override fun containingSymbol(symbol: KaSymbol): KaSymbol? = withValidityAssertion {
        computeContainingSymbol(symbol)
    }

    override fun containingDeclaration(symbol: KaSymbol): KaDeclarationSymbol? = withValidityAssertion {
        computeContainingDeclaration(symbol)
    }

    override fun containingFile(symbol: KaSymbol): KaFileSymbol? = withValidityAssertion {
        computeContainingFile(symbol)
    }

    override fun containingModule(symbol: KaSymbol): KaModule = withValidityAssertion {
        computeContainingModule(symbol)
    }

    override fun samConstructor(symbol: KaClassLikeSymbol): KaSamConstructorSymbol? = withValidityAssertion {
        val classId = symbol.classId ?: return null
        val owner = analysisSession.getClassLikeSymbol(classId) ?: return null
        val firSession = analysisSession.firSession
        val resolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))
        return resolver.getSamConstructor(owner)?.let {
            analysisSession.firSymbolBuilder.functionBuilder.buildSamConstructorSymbol(it.symbol)
        }
    }

    override fun functionalInterfaceFunction(symbol: KaClassLikeSymbol): KaNamedFunctionSymbol? = withValidityAssertion {
        val classId = symbol.classId ?: return null
        val firClassOrTypeAlias = analysisSession.getClassLikeSymbol(classId) ?: return null
        val firSession = analysisSession.firSession
        val resolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))
        val samFunction = resolver.getSamFunction(firClassOrTypeAlias) ?: return null
        return analysisSession.firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(samFunction)
    }

    override fun functionalInterface(symbol: KaSamConstructorSymbol): KaClassLikeSymbol = withValidityAssertion {
        symbol.firSymbol.fir.returnTypeRef.coneType.classId?.toLookupTag()?.let {
            analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByLookupTag(it)
        } ?: errorWithAttachment("Cannot retrieve functional interface for KaSamConstructorSymbol") {
            withSymbolAttachment("KaSamConstructorSymbol", analysisSession, symbol)
        }
    }

    override fun functionalInterfaceFunction(symbol: KaSamConstructorSymbol): KaNamedFunctionSymbol = withValidityAssertion {
        functionalInterfaceFunction(functionalInterface(symbol))
            ?: errorWithAttachment("SAM constructor should have a corresponding function in its functional interface") {
                withSymbolAttachment("KaSamConstructorSymbol", analysisSession, symbol)
                withSymbolAttachment("functionalInterface", analysisSession, functionalInterface(symbol))
            }
    }

    @KaExperimentalApi
    override fun originalConstructorIfTypeAliased(symbol: KaConstructorSymbol): KaConstructorSymbol? = withValidityAssertion {
        require(symbol is KaFirConstructorSymbol)

        val originalConstructor = symbol.firSymbol.typeAliasConstructorInfo?.originalConstructor as? FirConstructor ?: return null

        analysisSession.firSymbolBuilder.functionBuilder.buildConstructorSymbol(originalConstructor.symbol)
    }

    override fun allOverriddenSymbols(symbol: KaCallableSymbol): Sequence<KaCallableSymbol> = withValidityAssertion {
        symbol.handleOverriddenSymbols(
            forAccessorSymbol = { getAllOverriddenAccessorSymbols(it) },
            fallback = { getAllOverriddenSymbols(it) },
        )
    }

    override fun directlyOverriddenSymbols(symbol: KaCallableSymbol): Sequence<KaCallableSymbol> = withValidityAssertion {
        symbol.handleOverriddenSymbols(
            forAccessorSymbol = { getDirectlyOverriddenAccessorSymbols(it) },
            fallback = { getDirectlyOverriddenSymbols(it) },
        )
    }

    override fun isSubClassOf(symbol: KaClassSymbol, superClass: KaClassSymbol): Boolean = withValidityAssertion {
        return computeIsSubClassOf(symbol, superClass)
    }

    override fun isDirectSubClassOf(symbol: KaClassSymbol, superClass: KaClassSymbol): Boolean = withValidityAssertion {
        return computeIsDirectSubClassOf(symbol, superClass)
    }

    private inline fun KaCallableSymbol.handleOverriddenSymbols(
        forAccessorSymbol: (KaPropertyAccessorSymbol) -> Sequence<KaCallableSymbol>,
        fallback: (KaCallableSymbol) -> Sequence<KaCallableSymbol>,
    ): Sequence<KaCallableSymbol> = when (this) {
        is KaValueParameterSymbol -> this.primaryConstructorProperty?.let(fallback).orEmpty()
        is KaPropertyAccessorSymbol -> forAccessorSymbol(this)
        is KaNamedFunctionSymbol -> getSyntheticJavaPropertyAccessor(this)?.let(forAccessorSymbol) ?: fallback(this)
        else -> fallback(this)
    }

    override fun intersectionOverriddenSymbols(symbol: KaCallableSymbol): List<KaCallableSymbol> = withValidityAssertion {
        symbol.handleOverriddenSymbols(
            forAccessorSymbol = { getIntersectionOverriddenAccessorSymbols(it).asSequence() },
            fallback = { getIntersectionOverriddenSymbols(it).asSequence() },
        ).toList()
    }

    /**
     * Finds the synthetic property accessor corresponding to the given Java getter or setter method.
     *
     * When Kotlin accesses Java classes, it synthesizes properties from Java getter/setter method pairs
     * (e.g., `getField()`/`setField()` become a synthetic `field` property). This function performs
     * the reverse lookup: given a Java method symbol, it finds the corresponding accessor of the
     * synthetic property.
     *
     * This is used to compute overridden symbols for Java methods that participate in
     * synthetic property generation, and the synthetic property overrides a real Kotlin property.
     *
     * @param functionSymbol A function symbol representing a Java getter or setter method
     * @return The getter or setter of the synthetic property that wraps the given Java method, or `null` if not applicable
     */
    private fun getSyntheticJavaPropertyAccessor(functionSymbol: KaNamedFunctionSymbol): KaPropertyAccessorSymbol? {
        val origin = functionSymbol.origin
        if (!origin.isFromJava) return null

        return with(analysisSession) {
            val containingClass = functionSymbol.containingDeclaration as? KaClassSymbol ?: return null
            containingClass.combinedDeclaredMemberScope
                .findSyntheticJavaPropertyAccessor(functionSymbol.name) { propertySymbol, accessorKind, _ ->
                    if (accessorKind.getJavaAccessorSymbol(propertySymbol) == functionSymbol) {
                        accessorKind.getPropertyAccessorSymbol(propertySymbol)
                    } else {
                        null
                    }
                }
        }
    }

    override fun implementationState(
        symbol: KaCallableSymbol,
        implementerClassSymbol: KaClassSymbol,
    ): KaCallableImplementationState? = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        require(implementerClassSymbol is KaFirSymbol<*>)

        when (symbol) {
            is KaNamedFunctionSymbol, is KaPropertySymbol, is KaPropertyAccessorSymbol -> {
                val memberFirSymbol = symbol.firSymbol as? FirCallableSymbol<*> ?: return null

                val memberClassFirSymbol = memberFirSymbol.getContainingClassSymbol() as? FirClassSymbol<*> ?: return null
                memberClassFirSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

                val implementerClassFirSymbol = implementerClassSymbol.firSymbol as? FirClassSymbol<*> ?: return null
                implementerClassFirSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

                if (
                    memberClassFirSymbol != implementerClassFirSymbol &&
                    !memberClassFirSymbol.isSupertypeOf(implementerClassFirSymbol, rootModuleSession)
                ) {
                    return null
                }

                memberFirSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

                val scopeSession = analysisSession.getScopeSessionFor(analysisSession.firSession)
                with(SessionHolderImpl(rootModuleSession, scopeSession)) {
                    return memberFirSymbol.getImplementationStatus(implementerClassFirSymbol).toKaImplementationState()
                }
            }
            else -> {
                return null
            }
        }
    }

    override fun fakeOverrideOriginal(symbol: KaCallableSymbol): KaCallableSymbol = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        if (symbol is KaLocalVariableSymbol ||
            symbol is KaEnumEntrySymbol ||
            symbol is KaConstructorSymbol ||
            symbol is KaSamConstructorSymbol ||
            symbol is KaAnonymousFunctionSymbol
        ) {
            return symbol
        }

        // Fake-override attributes are placed on the first-class callables, not on their sub-callables (e.g., value parameters).
        // So for such callables we unwrap the enclosing callable and return the corresponding sub-callable from the base declaration.
        return when (symbol) {
            is KaReceiverParameterSymbol -> {
                val original = fakeOverrideOriginal(symbol.owningCallableSymbol)
                original.receiverParameter
            }
            is KaValueParameterSymbol -> {
                val owner = containingDeclaration(symbol) as? KaFunctionSymbol ?: return symbol
                val index = owner.valueParameters.indexOf(symbol)
                val original = fakeOverrideOriginal(owner) as? KaFunctionSymbol
                original?.valueParameters?.getOrNull(index)
            }
            is KaContextParameterSymbol -> {
                val owner = containingDeclaration(symbol) as? KaCallableSymbol ?: return symbol
                val index = owner.contextParameters.indexOf(symbol)
                val original = fakeOverrideOriginal(owner)
                original.contextParameters.getOrNull(index)
            }
            is KaPropertyAccessorSymbol -> {
                val owner = containingDeclaration(symbol) as? KaPropertySymbol ?: return symbol
                val original = fakeOverrideOriginal(owner) as? KaPropertySymbol ?: return symbol
                if (symbol is KaPropertyGetterSymbol) {
                    original.getter
                } else {
                    original.setter
                }
            }
            else -> {
                val originalDeclaration = symbol.firSymbol.fir as FirCallableDeclaration
                val unwrappedDeclaration = originalDeclaration.unwrapFakeOverridesOrDelegated()
                unwrappedDeclaration.buildSymbol(analysisSession.firSymbolBuilder) as KaCallableSymbol
            }
        } ?: symbol
    }


    override fun getExpectsForActual(symbol: KaDeclarationSymbol): List<KaDeclarationSymbol> = withValidityAssertion {
        computeExpectsForActual(symbol)
    }

    override fun sealedClassInheritors(symbol: KaNamedClassSymbol): List<KaNamedClassSymbol> = withValidityAssertion {
        require(symbol.modality == KaSymbolModality.SEALED)
        require(symbol is KaFirNamedClassSymbol)

        val inheritorClassIds = symbol.firSymbol.fir.getSealedClassInheritors(analysisSession.firSession)

        return with(analysisSession) {
            inheritorClassIds.mapNotNull { findClass(it) as? KaNamedClassSymbol }
        }
    }

    override fun hasConflictingSignatureWith(
        symbol: KaFunctionSymbol,
        other: KaFunctionSymbol,
        targetPlatform: TargetPlatform,
    ): Boolean = withValidityAssertion {
        val thisFirSymbol = symbol.firSymbol
        val otherFirSymbol = other.firSymbol

        val thisHasLowPriority = hasLowPriorityAnnotation(thisFirSymbol.resolvedAnnotationsWithClassIds)
        val otherHasLowPriority = hasLowPriorityAnnotation(otherFirSymbol.resolvedAnnotationsWithClassIds)
        if (thisHasLowPriority != otherHasLowPriority) {
            return false
        }

        /**
         * [FirDeclarationOverloadabilityHelper] performs signature comparison only from JVM platform perspective.
         * However, as the API needs to be more generic than that, here we perform manual signature comparison
         * before calling [FirDeclarationOverloadabilityHelper].
         * This is done to handle cases which are considered conflicting on JVM but completely valid on other platforms:
         * - Overloads by type parameters
         * ```kotlin
         * fun <T> foo() // Conflicting on JVM, valid on other platforms
         * fun foo()
         * ```
         * - Overloads by vararg/array parameters
         * ```kotlin
         * fun foo(vararg ints: Int) // Conflicting on JVM, valid on other platforms
         * fun foo(ints: IntArray)
         * ```
         */
        if (!targetPlatform.isJvm()) {
            if (thisFirSymbol.typeParameterSymbols.isEmpty() != otherFirSymbol.typeParameterSymbols.isEmpty()) {
                return false
            }

            val thisVarargParameterPosition = symbol.valueParameters.indexOfFirst { it.isVararg }
            val otherVarargParameterPosition = other.valueParameters.indexOfFirst { it.isVararg }
            if (thisVarargParameterPosition != otherVarargParameterPosition) {
                return false
            }
        }

        val overloadabilityHelper = analysisSession.firSession.declarationOverloadabilityHelper

        return if (analysisSession.firSession.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
            overloadabilityHelper.getContextParameterShadowing(thisFirSymbol, otherFirSymbol) == BothWays
        } else {
            overloadabilityHelper.isConflicting(
                thisFirSymbol,
                otherFirSymbol,
            )
        }
    }
}
