/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirGenericSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.scopes.substitutorForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

internal class KaFirSubstitutorProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaSubstitutorProvider, KaFirSessionComponent {
    override fun createInheritanceTypeSubstitutor(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): KaSubstitutor? {
        withValidityAssertion {
            if (subClass == superClass) return KaSubstitutor.Empty(token)

            val baseFirSymbol = subClass.firSymbol
            val superFirSymbol = superClass.firSymbol
            val inheritancePath = collectInheritancePath(baseFirSymbol, superFirSymbol) ?: return null
            val substitutors = inheritancePath.map { (type, symbol) ->
                type.substitutorForSuperType(rootModuleSession, symbol)
            }
            return when (substitutors.size) {
                0 -> KaSubstitutor.Empty(token)
                else -> {
                    val chained = substitutors.reduce { left, right -> left.chain(right) }
                    firSymbolBuilder.typeBuilder.buildSubstitutor(chained)
                }
            }
        }
    }

    private fun collectInheritancePath(
        baseSymbol: FirClassSymbol<*>,
        superSymbol: FirClassSymbol<*>,
    ): List<Pair<ConeClassLikeType, FirRegularClassSymbol>>? {
        val stack = mutableListOf<Pair<ConeClassLikeType, FirRegularClassSymbol>>()
        var result: List<Pair<ConeClassLikeType, FirRegularClassSymbol>>? = null

        fun dfs(symbol: FirClassSymbol<*>) {
            for (superType in symbol.resolvedSuperTypes) {
                if (result != null) {
                    return
                }
                if (superType !is ConeClassLikeType) continue
                val superClassSymbol = superType.toRegularClassSymbol(rootModuleSession) ?: continue
                stack += superType to superClassSymbol
                if (superClassSymbol == superSymbol) {
                    result = stack.toList()
                    check(stack.removeLast().second == superClassSymbol)
                    break
                }
                dfs(superClassSymbol)
                check(stack.removeLast().second == superClassSymbol)
            }
        }

        dfs(baseSymbol)
        return result?.reversed()
    }

    override fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor = withValidityAssertion {
        if (mappings.isEmpty()) return KaSubstitutor.Empty(token)

        val firSubstitution = buildMap {
            mappings.forEach { (ktTypeParameterSymbol, ktType) ->
                check(ktTypeParameterSymbol is KaFirTypeParameterSymbol)
                check(ktType is KaFirType)
                put(ktTypeParameterSymbol.firSymbol, ktType.coneType)
            }
        }

        return when (val coneSubstitutor = ConeSubstitutorByMap.create(firSubstitution, analysisSession.firSession)) {
            is ConeSubstitutorByMap -> KaFirMapBackedSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
            else -> KaFirGenericSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
        }
    }
}