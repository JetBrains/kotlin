package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSignature
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal fun FirScope.getCallableSymbols(callableNames: Collection<Name>, builder: KtSymbolByFirBuilder) = sequence {
    callableNames.forEach { name ->
        val callables = mutableListOf<KtCallableSymbol>()
        processFunctionsByName(name) { firSymbol ->
            callables.add(builder.functionLikeBuilder.buildFunctionSymbol(firSymbol))
        }
        processPropertiesByName(name) { firSymbol ->
            val symbol = when {
                firSymbol is FirPropertySymbol && firSymbol.fir.isSubstitutionOverride -> {
                    builder.variableLikeBuilder.buildVariableSymbol(firSymbol)
                }
                else -> builder.callableBuilder.buildCallableSymbol(firSymbol)
            }
            callables.add(symbol)
        }
        yieldAll(callables)
    }
}

internal fun FirScope.getCallableSignatures(callableNames: Collection<Name>, builder: KtSymbolByFirBuilder) = sequence {
    callableNames.forEach { name ->
        val signatures = mutableListOf<KtCallableSignature<*>>()
        processFunctionsByName(name) { firSymbol ->
            signatures.add(builder.functionLikeBuilder.buildFunctionLikeSignature(firSymbol))
        }
        processPropertiesByName(name) { firSymbol ->
            signatures.add(builder.variableLikeBuilder.buildVariableLikeSignature(firSymbol))
        }
        yieldAll(signatures)
    }
}

internal fun FirScope.getClassifierSymbols(classLikeNames: Collection<Name>, builder: KtSymbolByFirBuilder): Sequence<KtClassifierSymbol> =
    sequence {
        classLikeNames.forEach { name ->
            val classifierSymbols = mutableListOf<KtClassifierSymbol>()
            processClassifiersByName(name) { firSymbol ->
                classifierSymbols.add(builder.classifierBuilder.buildClassifierSymbol(firSymbol))
            }
            yieldAll(classifierSymbols)
        }
    }

internal fun FirScope.getConstructors(builder: KtSymbolByFirBuilder): Sequence<KtConstructorSymbol> =
    sequence {
        val constructorSymbols = mutableListOf<KtConstructorSymbol>()
        processDeclaredConstructors { firSymbol ->
            constructorSymbols.add(builder.functionLikeBuilder.buildConstructorSymbol(firSymbol))
        }
        yieldAll(constructorSymbols)
    }
