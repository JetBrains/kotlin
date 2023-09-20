/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.scopes.substitutorForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

internal class KtFirSubstitutorProvider(
    override val analysisSession: KtFirAnalysisSession,
) : KtSubstitutorProvider(), KtFirAnalysisSessionComponent {
    override fun createSubstitutor(
        subClass: KtClassOrObjectSymbol,
        superClass: KtClassOrObjectSymbol,
    ): KtSubstitutor? {
        if (subClass == superClass) return KtSubstitutor.Empty(token)

        val baseFirSymbol = subClass.firSymbol
        val superFirSymbol = superClass.firSymbol
        val inheritancePath = collectInheritancePath(baseFirSymbol, superFirSymbol) ?: return null
        val substitutors = inheritancePath.map { (type, symbol) ->
            type.substitutorForSuperType(rootModuleSession, symbol)
        }
        return when (substitutors.size) {
            0 -> KtSubstitutor.Empty(token)
            else -> {
                val chained = substitutors.reduce { left, right -> left.chain(right) }
                firSymbolBuilder.typeBuilder.buildSubstitutor(chained)
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

}