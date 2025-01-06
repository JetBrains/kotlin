/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
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

class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope,
    private val session: FirSession,
) : FirScope() {
    private val aliasedTypeExpansionGloballyEnabled: Boolean = session
        .languageVersionSettings
        .getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors wrapper@{ originalConstructorSymbol ->
            val typeParameters = typeAliasSymbol.fir.typeParameters

            processor(
                buildConstructorCopy(originalConstructorSymbol.fir) {
                    // Typealiased constructors point to the typealias source
                    // for the convenience of Analysis API
                    source = typeAliasSymbol.source

                    symbol = FirConstructorSymbol(originalConstructorSymbol.callableId)
                    origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor

                    // We consider typealiased constructors to be coming
                    // from the module of the typealias
                    moduleData = typeAliasSymbol.moduleData

                    this.typeParameters.clear()
                    typeParameters.mapTo(this.typeParameters) {
                        buildConstructedClassTypeParameterRef { symbol = it.symbol }
                    }

                    valueParameters.clear()
                    originalConstructorSymbol.fir.valueParameters.mapTo(valueParameters) { originalValueParameter ->
                        buildValueParameterCopy(originalValueParameter) {
                            symbol = FirValueParameterSymbol(originalValueParameter.name)
                            moduleData = typeAliasSymbol.moduleData
                            origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor
                            containingDeclarationSymbol = this@buildConstructorCopy.symbol
                        }
                    }

                    contextParameters.clear()
                    originalConstructorSymbol.fir.contextParameters.mapTo(contextParameters) { originalContextReceiver ->
                        buildValueParameterCopy(originalContextReceiver) {
                            symbol = FirValueParameterSymbol(originalContextReceiver.name)
                            moduleData = typeAliasSymbol.moduleData
                            origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor
                            containingDeclarationSymbol = this@buildConstructorCopy.symbol
                        }
                    }

                    if (aliasedTypeExpansionGloballyEnabled) {
                        returnTypeRef = returnTypeRef.withReplacedConeType(
                            returnTypeRef.coneType.withAbbreviation(AbbreviatedTypeAttribute(typeAliasSymbol.defaultType()))
                        )
                    }

                    val outerType = (typeAliasSymbol.resolvedExpandedTypeRef.coneType as? ConeClassLikeType)?.let {
                        outerType(it, session) { session.firProvider.getContainingClass(it) }
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
                                containingDeclarationSymbol = this@buildConstructorCopy.symbol
                            }
                        }
                    }
                }.apply {
                    originalConstructorIfTypeAlias = originalConstructorSymbol.fir
                    typeAliasForConstructor = typeAliasSymbol
                    if (delegatingScope is FirClassSubstitutionScope) {
                        typeAliasConstructorSubstitutor = delegatingScope.substitutor
                    }
                }.symbol
            )
        }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): TypeAliasConstructorsSubstitutingScope? {
        return delegatingScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, it, session)
        }
    }
}
