/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.symbols.IrSymbol

internal data class Variable(var state: State?)

internal typealias Fields = MutableMap<IrSymbol, State>
internal typealias Field = Pair<IrSymbol, State>