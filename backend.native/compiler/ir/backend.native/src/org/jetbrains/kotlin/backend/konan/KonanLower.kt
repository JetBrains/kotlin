package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.lower.*
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

        phaser.phase("Lower_builtin_operators") {
            BuiltinOperatorLowering(context).runOnFilePostfix(irFile)
        }

        phaser.phase("Lower_shared_variables") {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase("Lower_local_functions") {
            LocalFunctionsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase("Lower_callables") {
            CallableReferenceLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase("Autobox") {
            Autoboxing(context).lower(irFile)
        }
    }
}
