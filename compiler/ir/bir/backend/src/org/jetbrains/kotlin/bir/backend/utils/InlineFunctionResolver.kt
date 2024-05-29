/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.utils

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.jvm.isInlineFunctionCall
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.symbols.BirFunctionSymbol
import org.jetbrains.kotlin.bir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.resolveFakeOverride

abstract class InlineFunctionResolver {
    open val allowExternalInlining: Boolean
        get() = false

    open fun needsInlining(function: BirFunction) = function.isInline && (allowExternalInlining || !function.isExternal)

    open fun getFunctionDeclaration(symbol: BirFunctionSymbol): BirFunction? {
        TODO()
        /*if (shouldExcludeFunctionFromInlining(symbol)) return null

        val owner = symbol.owner
        return (owner as? BirSimpleFunction)?.resolveFakeOverride() ?: owner*/
    }

    protected open fun shouldExcludeFunctionFromInlining(symbol: BirFunctionSymbol): Boolean {
        TODO()
        //return !needsInlining(symbol.owner) || Symbols.isLateinitIsInitializedPropertyGetter(symbol) || Symbols.isTypeOfIntrinsic(symbol)
    }
}

context(JvmBirBackendContext)
class JvmInlineFunctionResolver() : InlineFunctionResolver() {
    override fun needsInlining(function: BirFunction): Boolean = function.isInlineFunctionCall()
}