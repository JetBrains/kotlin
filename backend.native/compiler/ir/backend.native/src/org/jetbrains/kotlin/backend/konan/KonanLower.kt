package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.PhaseManager
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.common.lower.LocalFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class KonanLower(val context: Context) {

    fun lower(module: IrModuleFragment) {
        module.files.forEach {
            lower(it)
        }
    }

    fun lower(irFile: IrFile) {
        val phaser = PhaseManager(context)

        phaser.phase("Lower_shared_variables") {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase("Lower_local_functions") {
            LocalFunctionsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase("Lower_callables") {
            CallableReferenceLowering(context).runOnFilePostfix(irFile)
        }
    }
}
