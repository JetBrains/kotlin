/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.phaser.makeCustomPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.expectDeclarationsRemovingPhase
import org.jetbrains.kotlin.backend.jvm.serializeIrFile
import org.jetbrains.kotlin.backend.jvm.serializeTopLevelIrClass
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource

internal val serializeIrPhase = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    { context, irModule -> SerializeIrPhase(context).lower(irModule) },
    name = "SerializeIr",
    description = "If specified by compiler options, save serialized IR in class annotations",
    prerequisite = setOf(expectDeclarationsRemovingPhase),
)

class SerializeIrPhase(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.state.configuration.getBoolean(JVMConfigurationKeys.SERIALIZE_IR)) {
            (irFile.metadata as? MetadataSource.File)?.serializedIr = serializeIrFile(context, irFile)

            for (irClass in irFile.declarations.filterIsInstance<IrClass>()) {
                (irClass.metadata as? MetadataSource.Class)?.serializedIr = serializeTopLevelIrClass(context, irClass)
            }
        }
    }
}