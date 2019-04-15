/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.name.Name

open class FirPropertySymbol(callableId: CallableId) : FirVariableSymbol(callableId), ConePropertySymbol

open class FirVariableSymbol(override val callableId: CallableId) : ConeVariableSymbol, FirCallableSymbol() {

    @Deprecated("TODO: Better solution for local vars?")
    constructor(name: Name) : this(CallableId(name))  // TODO?
}
