package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.lower.LocalFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class KonanLower(val context: KonanBackendContext) {

    fun lower(module: IrModuleFragment) {
        module.files.forEach {
            lower(it)
        }
    }

    fun lower(irFile: IrFile) {
        SharedVariablesLowering(context).runOnFilePostfix(irFile)
        LocalFunctionsLowering(context).runOnFilePostfix(irFile)
        CallableReferenceLowering(context).runOnFilePostfix(irFile)
    }
}