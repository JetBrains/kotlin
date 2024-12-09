/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import java.util.*

internal val FirCallableSymbol<*>.isTypeAliasedConstructor: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor

internal val FirValueParameterSymbol.isTypeAliasedConstructorParameter: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor

internal fun typeAliasedConstructorsEqual(left: FirConstructorSymbol, right: FirConstructorSymbol): Boolean {
    require(left.isTypeAliasedConstructor)
    require(right.isTypeAliasedConstructor)

    if (left == right) return true

    return left.typeAliasConstructorInfo == right.typeAliasConstructorInfo
}

internal fun typeAliasedConstructorParametersEqual(left: FirValueParameterSymbol, right: FirValueParameterSymbol): Boolean {
    require(left.isTypeAliasedConstructorParameter)
    require(right.isTypeAliasedConstructorParameter)

    if (left == right) return true

    val leftConstructor = left.containingDeclarationSymbol as? FirConstructorSymbol ?: return false
    val rightConstructor = right.containingDeclarationSymbol as? FirConstructorSymbol ?: return false

    val leftIndex = leftConstructor.valueParameterSymbols.indexOf(left)
    val rightIndex = rightConstructor.valueParameterSymbols.indexOf(right)

    return leftIndex == rightIndex && typeAliasedConstructorsEqual(leftConstructor, rightConstructor)
}

internal fun FirConstructorSymbol.hashCodeForTypeAliasedConstructor(): Int {
    require(isTypeAliasedConstructor)

    return typeAliasConstructorInfo!!.hashCode()
}

internal fun FirValueParameterSymbol.hashCodeForTypeAliasedConstructorParameter(): Int {
    require(isTypeAliasedConstructorParameter)

    val containingConstructor = containingDeclarationSymbol as FirConstructorSymbol
    val parameterIndex = containingConstructor.valueParameterSymbols.indexOf(this)

    return Objects.hash(parameterIndex, containingConstructor.typeAliasConstructorInfo)
}