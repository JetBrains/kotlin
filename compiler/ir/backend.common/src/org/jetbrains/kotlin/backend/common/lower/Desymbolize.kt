/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.desymbolize

val desymbolizePhase = makeIrModulePhase(
    ::Desymbolize,
    name = "Desymbolize",
    description = "Replace symbols by their owners in IR structures"
)

class Desymbolize() : FileLoweringPass {
    constructor (context: CommonBackendContext) : this()

    override fun lower(irFile: IrFile) {
        irFile.desymbolize()
    }
}