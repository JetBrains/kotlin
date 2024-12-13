/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.checkFunctionDispatchReceiver
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrFunction

internal object IrFunctionDispatchReceiverChecker : IrFunctionChecker {
    override fun check(
        declaration: IrFunction,
        context: CheckerContext,
    ) {
        declaration.checkFunctionDispatchReceiver(declaration, context)
    }
}