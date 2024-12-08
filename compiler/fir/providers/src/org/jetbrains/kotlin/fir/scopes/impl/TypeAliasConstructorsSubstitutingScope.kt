/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*

private object TypeAliasConstructorKey : FirDeclarationDataKey()

var <T : FirFunction> T.originalConstructorIfTypeAlias: T? by FirDeclarationDataRegistry.data(TypeAliasConstructorKey)
val <T : FirFunction> FirFunctionSymbol<T>.originalConstructorIfTypeAlias: T?
    get() = fir.originalConstructorIfTypeAlias

val FirFunctionSymbol<*>.isTypeAliasedConstructor: Boolean
    get() = fir.originalConstructorIfTypeAlias != null

private object TypeAliasForConstructorKey : FirDeclarationDataKey()

var FirFunction.typeAliasForConstructor: FirTypeAliasSymbol? by FirDeclarationDataRegistry.data(TypeAliasForConstructorKey)
val FirFunctionSymbol<*>.typeAliasForConstructor: FirTypeAliasSymbol?
    get() = fir.typeAliasForConstructor

private object TypeAliasConstructorSubstitutorKey : FirDeclarationDataKey()

var FirConstructor.typeAliasConstructorSubstitutor: ConeSubstitutor? by FirDeclarationDataRegistry.data(TypeAliasConstructorSubstitutorKey)

private object TypeAliasOuterType : FirDeclarationDataKey()

/**
 * It's not null for cases when typealias and its inner RHS have different containing declarations. For instance, the following code:
 *
 * ```kt
 * class Outer {
 *   inner class Inner
 * }
 * typealias OI = Outer.Inner
 * fun foo() { Outer().OI() }
 * ```
 *
 * Motivation: in such a case, we takes type alias from `PackageMemberScope` but `ScopeBasedTowerLevel` at this scope only handles
 * candidates with not null receivers.
 * To handle the case, we treat such typealias as a typealias with a fake extension receiver.
 *
 * Also, we should not forget to check the receiver type in `CheckExtensionReceiver` to detect potential type mismatches like this:
 *
 * ```kt
 * class Generic<T> {
 *     inner class Inner
 * }
 *
 * typealias GIntI = Generic<Int>.Inner
 *
 * fun <T> test5(x: Generic<T>) = x.GIntI() // UNRESOLVED_REFERENCE_WRONG_RECEIVER
 * ```
 *
 * The fake extension receiver is not applicable to not inner RHS,
 * because it's disallowed to call a constructor on a nested class-like declaration on an outer instance.
 *
 * Also, consider receiver with independent type arguments as outer.
 * To make it possible to report `UNRESOLVED_REFERENCE_WRONG_RECEIVER` for nested type aliases with inner RHS. For instance:
 *
 * ```kt
 * class Outer<T> {
 *     inner class Inner
 *     typealias NestedTAToIntInner = Outer<Int>.Inner
 *
 *     fun test() {
 *         NestedTAToIntInner() // Dispatch receivers mismatch: `Outer<T>` and `Outer<Int>`
 *     }
 * }
 * ```
 */
var FirConstructor.outerDispatchReceiverTypeIfTypeAliasWithInnerRHS: ConeClassLikeType? by FirDeclarationDataRegistry.data(TypeAliasOuterType)

// TODO: Integrate it to `FirScopeProvider` and implement caching of constructors (KT-72929)
class TypeAliasConstructorsSubstitutingScope private constructor(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope,
    private val session: FirSession,
) : FirScope() {
    companion object {
        fun initialize(
            typeAliasSymbol: FirTypeAliasSymbol,
            session: FirSession,
            scopeSession: ScopeSession
        ): TypeAliasConstructorsSubstitutingScope? {
            val expandedType = typeAliasSymbol.resolvedExpandedTypeRef.coneType as? ConeClassLikeType ?: return null
            val expandedTypeScope = expandedType.scope(
                session, scopeSession,
                CallableCopyTypeCalculator.DoNothing,
                // Must be `STATUS`; otherwise we can't create substitution overrides for constructor symbols,
                // which we need to map typealias arguments to the expanded type arguments, which happens when
                // we request declared constructor symbols from the scope returned below.
                // See: `LLFirPreresolvedReversedDiagnosticCompilerFE10TestDataTestGenerated.testTypealiasAnnotationWithFixedTypeArgument`
                requiredMembersPhase = FirResolvePhase.STATUS,
            ) ?: return null

            return TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, expandedTypeScope, session)
        }
    }

    private val aliasedTypeExpansionGloballyEnabled: Boolean = session
        .languageVersionSettings
        .getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors wrapper@{ originalConstructorSymbol ->
            val originalConstructor = originalConstructorSymbol.fir
            val newConstructorSymbol = FirConstructorSymbol(originalConstructorSymbol.callableId)

            buildConstructor {
                symbol = newConstructorSymbol

                // Typealiased constructors point to the typealias source for the convenience of Analysis API
                source = typeAliasSymbol.source
                resolvePhase = originalConstructor.resolvePhase
                // We consider typealiased constructors to be coming from the module of the typealias
                moduleData = typeAliasSymbol.moduleData
                origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor
                attributes = originalConstructor.attributes.copy()

                typeAliasSymbol.fir.typeParameters.mapTo(typeParameters) {
                    buildConstructedClassTypeParameterRef { symbol = it.symbol }
                }

                status = originalConstructor.status

                returnTypeRef = originalConstructor.returnTypeRef.let {
                    if (aliasedTypeExpansionGloballyEnabled) {
                        it.withReplacedConeType(it.coneType.withAbbreviation(AbbreviatedTypeAttribute(typeAliasSymbol.defaultType())))
                    } else {
                        it
                    }
                }
                receiverParameter = originalConstructor.receiverParameter
                deprecationsProvider = originalConstructor.deprecationsProvider
                containerSource = originalConstructor.containerSource
                dispatchReceiverType = originalConstructor.dispatchReceiverType

                originalConstructor.contextParameters.mapTo(contextParameters) {
                    buildValueParameterCopy(it) {
                        symbol = FirValueParameterSymbol(it.name)
                        moduleData = typeAliasSymbol.moduleData
                        origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor
                        containingDeclarationSymbol = newConstructorSymbol
                    }
                }

                originalConstructor.valueParameters.mapTo(valueParameters) {
                    buildValueParameterCopy(it) {
                        symbol = FirValueParameterSymbol(it.name)
                        moduleData = typeAliasSymbol.moduleData
                        origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor
                        containingDeclarationSymbol = newConstructorSymbol
                    }
                }

                contractDescription = originalConstructor.contractDescription
                annotations.addAll(originalConstructor.annotations)
                delegatedConstructor = originalConstructor.delegatedConstructor
                body = originalConstructor.body
            }.apply {
                originalConstructorIfTypeAlias = originalConstructorSymbol.fir
                typeAliasForConstructor = typeAliasSymbol
                if (delegatingScope is FirClassSubstitutionScope) {
                    typeAliasConstructorSubstitutor = delegatingScope.substitutor
                }

                val expandedClassType = typeAliasSymbol.resolvedExpandedTypeRef.coneType
                val expandedClassSymbol = expandedClassType.toRegularClassSymbol(typeAliasSymbol.moduleData.session)
                if (expandedClassType is ConeClassLikeType && expandedClassSymbol?.isInner == true) {
                    val typeAliasRHSContainingClassSymbol = expandedClassSymbol.getContainingClassLookupTag()?.toRegularClassSymbol(session)

                    if (typeAliasRHSContainingClassSymbol != null) {
                        val outerTypeArguments = getOuterTypeArguments(expandedClassType, expandedClassSymbol)

                        val typeAliasContainingClassTag = typeAliasSymbol.getContainingClassLookupTag()
                        if (typeAliasContainingClassTag != typeAliasRHSContainingClassSymbol.toLookupTag() || outerTypeArguments.isNotEmpty()) {
                            outerDispatchReceiverTypeIfTypeAliasWithInnerRHS =
                                typeAliasRHSContainingClassSymbol.constructType(outerTypeArguments.toTypedArray())
                        }
                    }
                }
            }

            processor(newConstructorSymbol)
        }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): TypeAliasConstructorsSubstitutingScope? {
        return delegatingScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, it, session)
        }
    }
}
