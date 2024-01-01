/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal fun convertArgument(argument: Any?, analysisSession: KtFirAnalysisSession): Any? {
    return convertArgument(argument, analysisSession.firSymbolBuilder)
}

private fun convertArgument(argument: Any?, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return when (argument) {
        null -> null
        is FirRegularClass -> convertArgument(argument, firSymbolBuilder)
        is FirValueParameterSymbol -> convertArgument(argument, firSymbolBuilder)
        is FirEnumEntrySymbol -> convertArgument(argument, firSymbolBuilder)
        is FirRegularClassSymbol -> convertArgument(argument, firSymbolBuilder)
        is FirNamedFunctionSymbol -> convertArgument(argument, firSymbolBuilder)
        is FirPropertySymbol -> convertArgument(argument, firSymbolBuilder)
        is FirBackingFieldSymbol -> convertArgument(argument, firSymbolBuilder)
        is FirVariableSymbol<*> -> convertArgument(argument, firSymbolBuilder)
        is FirTypeParameterSymbol -> convertArgument(argument, firSymbolBuilder)
        is FirCallableSymbol<*> -> convertArgument(argument, firSymbolBuilder)
        is FirClassSymbol<*> -> convertArgument(argument, firSymbolBuilder)
        is FirClassLikeSymbol<*> -> convertArgument(argument, firSymbolBuilder)
        is FirBasedSymbol<*> -> convertArgument(argument, firSymbolBuilder)
        is FirClass -> convertArgument(argument, firSymbolBuilder)
        is FirTypeParameter -> convertArgument(argument, firSymbolBuilder)
        is FirValueParameter -> convertArgument(argument, firSymbolBuilder)
        is FirFunction -> convertArgument(argument, firSymbolBuilder)
        is FirCallableDeclaration -> convertArgument(argument, firSymbolBuilder)
        is FirMemberDeclaration -> convertArgument(argument, firSymbolBuilder)
        is FirDeclaration -> convertArgument(argument, firSymbolBuilder)
        is FirQualifiedAccessExpression -> convertArgument(argument, firSymbolBuilder)
        is FirExpression -> convertArgument(argument, firSymbolBuilder)
        is ConeKotlinType -> convertArgument(argument, firSymbolBuilder)
        is FirTypeRef -> convertArgument(argument, firSymbolBuilder)
        is KtSourceElement -> convertArgument(argument, firSymbolBuilder)
        is Map<*, *> -> convertArgument(argument, firSymbolBuilder)
        is Collection<*> -> convertArgument(argument, firSymbolBuilder)
        is Pair<*, *> -> convertArgument(argument, firSymbolBuilder)
        else -> argument
    }
}

private fun convertArgument(argument: FirRegularClass, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(argument.symbol) as KtNamedClassOrObjectSymbol
}

private fun convertArgument(argument: FirValueParameterSymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument)
}

private fun convertArgument(argument: FirEnumEntrySymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument)
}

private fun convertArgument(argument: FirRegularClassSymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(argument)
}

private fun convertArgument(argument: FirNamedFunctionSymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(argument)
}

private fun convertArgument(argument: FirPropertySymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(argument)
}

private fun convertArgument(argument: FirBackingFieldSymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(argument.fir.propertySymbol)
}

private fun convertArgument(argument: FirVariableSymbol<*>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(argument)
}

private fun convertArgument(argument: FirTypeParameterSymbol, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(argument)
}

private fun convertArgument(argument: FirCallableSymbol<*>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.callableBuilder.buildCallableSymbol(argument)
}

private fun convertArgument(argument: FirClassSymbol<*>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(argument)
}

private fun convertArgument(argument: FirClassLikeSymbol<*>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(argument)
}

private fun convertArgument(argument: FirBasedSymbol<*>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument)
}

private fun convertArgument(argument: FirClass, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(argument.symbol)
}

private fun convertArgument(argument: FirTypeParameter, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(argument.symbol)
}

private fun convertArgument(argument: FirValueParameter, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument.symbol)
}

private fun convertArgument(argument: FirFunction, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument)
}

private fun convertArgument(argument: FirCallableDeclaration, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.callableBuilder.buildCallableSymbol(argument.symbol)
}

private fun convertArgument(argument: FirMemberDeclaration, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument as FirDeclaration)
}

private fun convertArgument(argument: FirDeclaration, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.buildSymbol(argument)
}

private fun convertArgument(argument: FirQualifiedAccessExpression, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return argument.source!!.psi as KtExpression
}

private fun convertArgument(argument: FirExpression, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return argument.source!!.psi as KtExpression
}

private fun convertArgument(argument: ConeKotlinType, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.typeBuilder.buildKtType(argument)
}

private fun convertArgument(argument: FirTypeRef, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return firSymbolBuilder.typeBuilder.buildKtType(argument)
}

private fun convertArgument(argument: KtSourceElement, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return (argument as KtPsiSourceElement).psi
}

private fun convertArgument(argument: Map<*, *>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return argument.mapKeys { (key, _) ->
        convertArgument(key, firSymbolBuilder)
    }.mapValues { (_, value) -> 
        convertArgument(value, firSymbolBuilder)
    }
}

private fun convertArgument(argument: Collection<*>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return argument.map { value ->
        convertArgument(value, firSymbolBuilder)
    }
}

private fun convertArgument(argument: Pair<*, *>, firSymbolBuilder: KtSymbolByFirBuilder): Any? {
    return convertArgument(argument.first, firSymbolBuilder) to convertArgument(argument.second, firSymbolBuilder)
}

