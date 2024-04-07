/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

interface FirOverrideChecker : FirSessionComponent {
    fun isOverriddenFunction(
        overrideCandidate: FirSimpleFunction,
        baseDeclaration: FirSimpleFunction
    ): Boolean

    fun isOverriddenProperty(
        overrideCandidate: FirCallableDeclaration, // NB: in Java it can be a function which overrides accessor
        baseDeclaration: FirProperty
    ): Boolean

    fun chooseIntersectionVisibility(
        overrides: Collection<FirCallableSymbol<*>>,
        dispatchClassSymbol: FirRegularClassSymbol?,
    ): Visibility
}

fun FirOverrideChecker.isOverriddenFunction(
    overrideCandidate: FirNamedFunctionSymbol,
    baseDeclaration: FirNamedFunctionSymbol
): Boolean = isOverriddenFunction(overrideCandidate.fir, baseDeclaration.fir)

fun FirOverrideChecker.isOverriddenProperty(
    overrideCandidate: FirCallableSymbol<*>,
    baseDeclaration: FirPropertySymbol
): Boolean = isOverriddenProperty(overrideCandidate.fir, baseDeclaration.fir)

val FirSession.firOverrideChecker: FirOverrideChecker by FirSession.sessionComponentAccessor()
