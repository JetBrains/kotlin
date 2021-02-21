/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

/**
 * This pass removes all declarations with `isExpect == true`.
 */
class ExpectDeclarationsRemoveLowering(context: BackendContext) : DeclarationTransformer {
    private val remover = ExpectDeclarationRemover(context.ir.symbols.externalSymbolTable, true)

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return remover.transformFlat(declaration)
    }
}
