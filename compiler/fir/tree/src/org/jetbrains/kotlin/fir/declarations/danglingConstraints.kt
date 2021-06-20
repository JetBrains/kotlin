/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.name.Name

/**
 * Constraint without corresponding type argiment
 */
data class DanglingTypeConstraint(val name: Name, val source: FirSourceElement)

private object DanglingTypeConstraintsKey : FirDeclarationDataKey()

var <T> T.danglingTypeConstraints: List<DanglingTypeConstraint>?
        where T : FirDeclaration<T>, T : FirTypeParameterRefsOwner
        by FirDeclarationDataRegistry.data(DanglingTypeConstraintsKey)

fun FirDeclaration<*>.getDanglingTypeConstraintsOrEmpty(): List<DanglingTypeConstraint> {
    val res = when (this) {
        is FirRegularClass -> danglingTypeConstraints
        is FirSimpleFunction -> danglingTypeConstraints
        is FirProperty -> danglingTypeConstraints
        else -> null
    }
    return res ?: emptyList()
}
