/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

data class TailSuspendCalls(val callSites: Set<IrCall>, val hasNotTailSuspendCalls: Boolean)

/**
 * Collects calls to be treated as tail calls: "last" expressions which are either direct return statement with a call
 * to other suspend function, or, for a Unit-returning function, a call to other suspend function, also returning Unit.
 */
fun collectTailSuspendCalls(context: CommonBackendContext, irFunction: IrSimpleFunction): TailSuspendCalls { TODO() }
