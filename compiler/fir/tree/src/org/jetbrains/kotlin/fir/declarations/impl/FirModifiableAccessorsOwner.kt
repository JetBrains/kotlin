/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol

interface FirModifiableAccessorsOwner {
    var getter: FirPropertyAccessor?

    var setter: FirPropertyAccessor?

    val delegateFieldSymbol: FirDelegateFieldSymbol<*>?
}