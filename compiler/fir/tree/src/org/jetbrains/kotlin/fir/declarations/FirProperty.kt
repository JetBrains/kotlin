/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable

// May be should not inherit FirVariable
interface FirProperty : FirCallableMember, FirVariable {
    // Should it be nullable or have some default?
    val getter: FirPropertyAccessor

    val setter: FirPropertyAccessor

    val delegate: FirExpression?
}