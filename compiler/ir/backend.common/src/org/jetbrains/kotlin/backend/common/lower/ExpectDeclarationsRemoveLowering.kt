/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.ExpectDeclarationRemover
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * This pass removes all declarations with `isExpect == true`.
 */
class ExpectDeclarationsRemoveLowering(val context: BackendContext) : FileLoweringPass {

    val visitor = ExpectDeclarationRemover(context.ir.symbols.externalSymbolTable, true)

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(visitor)
    }
}
