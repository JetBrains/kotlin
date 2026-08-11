/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

private object ResultVariablesKey : FirDeclarationDataKey()

private var FirAnonymousFunction.resultVariablesData: MutableSet<FirPropertySymbol>? by FirDeclarationDataRegistry.data(ResultVariablesKey)

val FirAnonymousFunction.resultVariables: Set<FirPropertySymbol> get() = resultVariablesData ?: emptySet()

private val FirAnonymousFunctionSymbol.resultVariablesData: MutableSet<FirPropertySymbol>? by FirDeclarationDataRegistry
    .symbolAccessor(ResultVariablesKey)

val FirAnonymousFunctionSymbol.resultVariables: Set<FirPropertySymbol> get() = resultVariablesData ?: emptySet()

fun FirAnonymousFunction.addResultVariable(variable: FirPropertySymbol) {
    resultVariablesData?.add(variable) ?: run { resultVariablesData = mutableSetOf(variable) }
}

fun FirAnonymousFunction.addResultVariables(variables: Set<FirPropertySymbol>) {
    resultVariablesData?.addAll(variables) ?: run { resultVariablesData = variables.toMutableSet() }
}
