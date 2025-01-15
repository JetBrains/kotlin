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
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
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

class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope,
    private val session: FirSession,
) : FirScope() {
    companion object {
        fun initialize(
            typeAliasSymbol: FirTypeAliasSymbol,
            session: FirSession,
            scopeSession: ScopeSession,
        ): FirScope {
            val expandedType = typeAliasSymbol.resolvedExpandedTypeRef.coneType.fullyExpandedType(session)
            val expandedTypeScope = expandedType.scope(
                session, scopeSession,
                CallableCopyTypeCalculator.DoNothing,
                // Must be `STATUS`; otherwise we can't create substitution overrides for constructor symbols,
                // which we need to map typealias arguments to the expanded type arguments, which happens when
                // we request declared constructor symbols from the scope returned below.
                // See: `LLFirPreresolvedReversedDiagnosticCompilerFE10TestDataTestGenerated.testTypealiasAnnotationWithFixedTypeArgument`
                requiredMembersPhase = FirResolvePhase.STATUS,
            ) ?: return FirTypeScope.Empty

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

                deprecationsProvider = originalConstructor.deprecationsProvider
                containerSource = originalConstructor.containerSource

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

                returnTypeRef = originalConstructor.returnTypeRef.let {
                    if (aliasedTypeExpansionGloballyEnabled) {
                        it.withReplacedConeType(it.coneType.withAbbreviation(AbbreviatedTypeAttribute(typeAliasSymbol.defaultType())))
                    } else {
                        it
                    }
                }

                contractDescription = originalConstructor.contractDescription
                annotations.addAll(originalConstructor.annotations)
                delegatedConstructor = originalConstructor.delegatedConstructor
                body = originalConstructor.body

                val outerType = (typeAliasSymbol.resolvedExpandedTypeRef.coneType as? ConeClassLikeType)?.let { expandedType ->
                    outerType(expandedType, session) { session.firProvider.getContainingClass(it) }
                }
                if (outerType != null) {
                    // If the matched symbol is a type alias, and the expanded type is a nested class, e.g.,
                    //
                    //   class Outer {
                    //     inner class Inner
                    //   }
                    //   typealias OI = Outer.Inner
                    //   fun foo() { Outer().OI() }
                    //
                    // the chances are that `processor` belongs to [ScopeTowerLevel] (to resolve type aliases at top-level), which treats
                    // the explicit receiver (`Outer()`) as an extension receiver, whereas the constructor of the nested class may regard
                    // the same explicit receiver as a dispatch receiver (hence inconsistent receiver).
                    // Here, we add a copy of the nested class constructor, along with the outer type as an extension receiver, so that it
                    // can be seen as if resolving:
                    //
                    //   fun Outer.OI(): OI = ...
                    //
                    //
                    receiverParameter = originalConstructorSymbol.fir.returnTypeRef.withReplacedConeType(outerType).let {
                        buildReceiverParameter {
                            typeRef = it
                            symbol = FirReceiverParameterSymbol()
                            moduleData = typeAliasSymbol.moduleData
                            origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor
                            containingDeclarationSymbol = newConstructorSymbol
                        }
                    }
                }

                // Never treat typealiased constructors as class members, they can be only an extension or a regular function
                dispatchReceiverType = null
            }.apply {
                originalConstructorIfTypeAlias = originalConstructorSymbol.fir
                typeAliasForConstructor = typeAliasSymbol
                if (delegatingScope is FirClassSubstitutionScope) {
                    typeAliasConstructorSubstitutor = delegatingScope.substitutor
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
