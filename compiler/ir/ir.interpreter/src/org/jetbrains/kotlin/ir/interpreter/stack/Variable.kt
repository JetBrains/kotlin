/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.symbols.IrSymbol

// TODO maybe switch to typealias and use map instead of list
internal data class Variable(val symbol: IrSymbol) {
    lateinit var state: State

    constructor(symbol: IrSymbol, state: State) : this(symbol) {
        this.state = state
    }
}