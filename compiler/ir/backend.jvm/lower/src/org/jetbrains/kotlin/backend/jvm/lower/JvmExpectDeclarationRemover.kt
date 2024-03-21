/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationRemover
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.isExpect

@PhaseDescription(
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)
internal class JvmExpectDeclarationRemover(private val context: JvmBackendContext) : ExpectDeclarationRemover(context) {
    override fun lower(irFile: IrFile) {
        if (context.config.useFir) {
            irFile.declarations.removeIf { it.isExpect }
        } else {
            super.lower(irFile)
        }
    }
}
