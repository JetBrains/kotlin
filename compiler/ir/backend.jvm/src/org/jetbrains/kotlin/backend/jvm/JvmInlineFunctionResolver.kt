/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.InlineFunctionResolver
import org.jetbrains.kotlin.backend.common.InlineMode
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf

class JvmInlineFunctionResolver(private val context: JvmBackendContext) : InlineFunctionResolver() {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol, inlineMode: InlineMode): IrFunction? {
        return symbol.owner.resolveFakeOverrideOrSelf().takeIf { it.isInlineFunctionCall(context) }
    }
}
