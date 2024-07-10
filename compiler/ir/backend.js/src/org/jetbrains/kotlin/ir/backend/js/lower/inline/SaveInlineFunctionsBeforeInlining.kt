/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

// TODO: KT-67220: consider removing it
internal class SaveInlineFunctionsBeforeInlining(context: JsIrBackendContext) : DeclarationTransformer {
    private val inlineFunctionsBeforeInlining = context.mapping.inlineFunctionsBeforeInlining

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrFunction && declaration.isInline) {
            inlineFunctionsBeforeInlining[declaration] = declaration.deepCopyWithSymbols(declaration.parent)
        }

        return null
    }
}

internal class JsInlineFunctionResolver(
    context: JsIrBackendContext,
    override val inlineOnlyPrivateFunctions: Boolean
) : InlineFunctionResolverReplacingCoroutineIntrinsics<JsIrBackendContext>(context) {
    private val enumEntriesIntrinsic = context.intrinsics.enumEntriesIntrinsic
    private val inlineFunctionsBeforeInlining = context.mapping.inlineFunctionsBeforeInlining

    override val allowExternalInlining: Boolean = true

    override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        // TODO: After the expect fun enumEntriesIntrinsic become non-inline function, the code will be removed
        return symbol == enumEntriesIntrinsic || super.shouldExcludeFunctionFromInlining(symbol)
    }

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null
        return inlineFunctionsBeforeInlining[function] ?: return function
    }
}
