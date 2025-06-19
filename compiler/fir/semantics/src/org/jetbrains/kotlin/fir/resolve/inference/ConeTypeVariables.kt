/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.name.SpecialNames

class ConeTypeVariableForPostponedAtom(
    name: String,
    containsErrorComponent: Boolean,
) : ConeTypeVariable(name, containsErrorComponent)

class ConeTypeVariableForLambdaParameterType(
    name: String,
    containsErrorComponent: Boolean,
) : ConeTypeVariable(name, containsErrorComponent)

class ConeTypeVariableForLambdaReturnType(
    val argument: FirAnonymousFunction, name: String,
    containsErrorComponent: Boolean,
) : ConeTypeVariable(name, containsErrorComponent)

class ConeTypeParameterBasedTypeVariable(
    val typeParameterSymbol: FirTypeParameterSymbol,
    containsErrorComponent: Boolean,
) : ConeTypeVariable(
    SpecialNames.safeIdentifier(typeParameterSymbol.name).identifier,
    containsErrorComponent,
    typeParameterSymbol.toLookupTag()
)
