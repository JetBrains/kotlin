/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun FirElement.isInPlaceLambda(): Boolean {
    contract {
        returns(true) implies (this@isInPlaceLambda is FirAnonymousFunction)
    }
    return this is FirAnonymousFunction && isLambda && invocationKind != null
}

@OptIn(ExperimentalContracts::class)
fun FirElement.isLambda(): Boolean {
    contract {
        returns(true) implies (this@isLambda is FirAnonymousFunction)
    }
    return this is FirAnonymousFunction && isLambda
}