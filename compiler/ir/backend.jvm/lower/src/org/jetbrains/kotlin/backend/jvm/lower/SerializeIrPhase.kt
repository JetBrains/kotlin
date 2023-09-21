/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.expectDeclarationsRemovingPhase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.MetadataSource

internal val serializeIrPhase = makeIrModulePhase(
    lowering = ::SerializeIrPhase,
    name = "SerializeIr",
    description = "If specified by compiler options, save serialized IR in class annotations",
    prerequisite = setOf(expectDeclarationsRemovingPhase),
)

class SerializeIrPhase(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        context.irSerializer?.let { irSerializer ->
            (irFile.metadata as? MetadataSource.File)?.serializedIr = irSerializer.serializeIrFile(irFile)

            for (irClass in irFile.declarations.filterIsInstance<IrClass>()) {
                (irClass.metadata as? MetadataSource.Class)?.serializedIr = irSerializer.serializeTopLevelIrClass(irClass)
            }
        }
    }
}