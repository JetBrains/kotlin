/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.validateIrFile
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IrSymbolBindingChecker
import org.jetbrains.kotlin.ir.util.replaceUnboundSymbols
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class KonanLower(val context: Context) {

    fun lower() {
        // Phases to run against whole module.
        lowerModule(context.irModule!!)

        // Phases to run against a file.
        context.irModule!!.files.forEach {
            lowerFile(it)
        }
    }

    fun lowerModule(irModule: IrModuleFragment) {
        val phaser = PhaseManager(context)

        phaser.phase(KonanPhase.LOWER_INLINE_CONSTRUCTORS) {
            InlineConstructorsTransformation(context).lower(irModule)
        }

        // Inlining must be run before other phases.
        phaser.phase(KonanPhase.LOWER_INLINE) {
            FunctionInlining(context).inline(irModule)
            irModule.replaceUnboundSymbols(context)
            irModule.checkSymbolsBound()
        }
    }

    fun lowerFile(irFile: IrFile) {
        val phaser = PhaseManager(context)

        phaser.phase(KonanPhase.LOWER_STRING_CONCAT) {
            StringConcatenationLowering(context).lower(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_ENUMS) {
            EnumClassLowering(context).run(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_INITIALIZERS) {
            InitializersLowering(context).runOnFilePostfix(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_SHARED_VARIABLES) {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_DELEGATION) {
            PropertyDelegationLowering(context).lower(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_LOCAL_FUNCTIONS) {
            LocalDeclarationsLowering(context).runOnFilePostfix(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_TAILREC) {
            TailrecLowering(context).runOnFilePostfix(irFile)
            irFile.checkSymbolsBound()
        }
        phaser.phase(KonanPhase.LOWER_FINALLY) {
            FinallyBlocksLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DEFAULT_PARAMETER_EXTENT) {
            DefaultArgumentStubGenerator(context).runOnFilePostfix(irFile)
            DefaultParameterInjector(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_LATEINIT) {
            LateinitLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_BUILTIN_OPERATORS) {
            BuiltinOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INNER_CLASSES) {
            InnerClassLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INTEROP) {
            InteropLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_CALLABLES) {
            CallableReferenceLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_VARARG) {
            VarargInjectionLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_COROUTINES) {
            SuspendFunctionsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TYPE_OPERATORS) {
            TypeOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.BRIDGES_BUILDING) {
            BridgesBuilding(context).runOnFilePostfix(irFile)
            DirectBridgesCallsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.AUTOBOX) {
            validateIrFile(context, irFile)
            Autoboxing(context).lower(irFile)
        }
    }

    private fun IrElement.checkSymbolsBound() {
        if (context.shouldVerifyIr()) {
            this.acceptVoid(IrSymbolBindingChecker())
        }
    }
}
