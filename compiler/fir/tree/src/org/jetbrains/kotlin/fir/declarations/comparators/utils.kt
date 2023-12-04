/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.comparators

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.render
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
internal inline fun <T : Comparable<T>> ifNotEqual(lhs: T, rhs: T, then: (Int) -> Nothing) {
    contract {
        callsInPlace(then, InvocationKind.AT_MOST_ONCE)
    }
    val comparisonResult = lhs.compareTo(rhs)
    if (comparisonResult != 0) then(comparisonResult)
}

@OptIn(ExperimentalContracts::class)
internal inline fun ifRendersNotEqual(lhs: FirElement, rhs: FirElement, then: (Int) -> Nothing) {
    contract {
        callsInPlace(then, InvocationKind.AT_MOST_ONCE)
    }
    ifNotEqual(lhs.render(), rhs.render(), then)
}
