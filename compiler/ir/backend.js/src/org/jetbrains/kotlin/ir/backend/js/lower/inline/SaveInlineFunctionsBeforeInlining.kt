/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.inline.DefaultInlineFunctionResolver
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

internal class SaveInlineFunctionsBeforeInlining(context: JsIrBackendContext) : DeclarationTransformer {
    private val inlineFunctionsBeforeInlining = context.mapping.inlineFunctionsBeforeInlining

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrFunction && declaration.isInline) {
            inlineFunctionsBeforeInlining[declaration] = declaration.deepCopyWithVariables().also {
                it.patchDeclarationParents(declaration.parent)
            }
        }

        return null
    }
}

internal class JsInlineFunctionResolver(context: JsIrBackendContext) : DefaultInlineFunctionResolver(context) {
    private val enumEntriesIntrinsic = context.intrinsics.enumEntriesIntrinsic
    private val inlineFunctionsBeforeInlining = context.mapping.inlineFunctionsBeforeInlining
    private val inlineFunctionsBeforeInliningSymbols = hashMapOf<IrFunction, IrFunctionSymbol>()

    override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        // TODO: After the expect fun enumEntriesIntrinsic become non-inline function, the code will be removed
        return symbol == enumEntriesIntrinsic || super.shouldExcludeFunctionFromInlining(symbol)
    }

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {
        val function = super.getFunctionDeclaration(symbol)
        val functionBeforeInlining = inlineFunctionsBeforeInlining[function] ?: return function
        inlineFunctionsBeforeInliningSymbols[functionBeforeInlining] = function.symbol
        return functionBeforeInlining
    }

    override fun getFunctionSymbol(irFunction: IrFunction): IrFunctionSymbol {
        return inlineFunctionsBeforeInliningSymbols[irFunction] ?: super.getFunctionSymbol(irFunction)
    }
}
