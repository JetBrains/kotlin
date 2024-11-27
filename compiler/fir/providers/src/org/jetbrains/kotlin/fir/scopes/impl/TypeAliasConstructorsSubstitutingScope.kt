/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*

private object TypeAliasConstructorKey : FirDeclarationDataKey()

var FirConstructor.originalConstructorIfTypeAlias: FirConstructor? by FirDeclarationDataRegistry.data(TypeAliasConstructorKey)
val FirConstructorSymbol.isTypeAliasedConstructor: Boolean
    get() = fir.originalConstructorIfTypeAlias != null

private object TypeAliasForConstructorKey : FirDeclarationDataKey()

var FirConstructor.typeAliasForConstructor: FirTypeAliasSymbol? by FirDeclarationDataRegistry.data(TypeAliasForConstructorKey)
val FirConstructorSymbol.typeAliasForConstructor: FirTypeAliasSymbol?
    get() = fir.typeAliasForConstructor

private object TypeAliasConstructorSubstitutorKey : FirDeclarationDataKey()

var FirConstructor.typeAliasConstructorSubstitutor: ConeSubstitutor? by FirDeclarationDataRegistry.data(TypeAliasConstructorSubstitutorKey)

private object TypeAliasOuterType : FirDeclarationDataKey()

var FirConstructor.outerTypeIfTypeAlias: ConeClassLikeType? by FirDeclarationDataRegistry.data(TypeAliasOuterType)

class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope,
    private val outerType: ConeClassLikeType?,
    private val abbreviation: ConeClassLikeType?,
) : FirScope() {
    private val aliasedTypeExpansionGloballyEnabled: Boolean = typeAliasSymbol
        .moduleData
        .session
        .languageVersionSettings
        .getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors wrapper@{ originalConstructorSymbol ->
            val typeParameters = typeAliasSymbol.fir.typeParameters

            processor(
                buildConstructorCopy(originalConstructorSymbol.fir) {
                    symbol = FirConstructorSymbol(originalConstructorSymbol.callableId)
                    origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor

                    this.typeParameters.clear()
                    typeParameters.mapTo(this.typeParameters) { buildConstructedClassTypeParameterRef { symbol = it.symbol } }

                    if (abbreviation != null && aliasedTypeExpansionGloballyEnabled) {
                        returnTypeRef = returnTypeRef.withReplacedConeType(
                            returnTypeRef.coneType.withAbbreviation(AbbreviatedTypeAttribute(abbreviation))
                        )
                    }
                }.apply {
                    originalConstructorIfTypeAlias = originalConstructorSymbol.fir
                    typeAliasForConstructor = typeAliasSymbol
                    if (delegatingScope is FirClassSubstitutionScope) {
                        typeAliasConstructorSubstitutor = delegatingScope.substitutor
                    }
                    outerTypeIfTypeAlias = outerType
                }.symbol
            )
        }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): TypeAliasConstructorsSubstitutingScope? {
        return delegatingScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, it, outerType, abbreviation)
        }
    }
}
