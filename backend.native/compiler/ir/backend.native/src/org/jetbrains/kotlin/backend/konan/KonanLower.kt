/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectDeclarationsRemoving
import org.jetbrains.kotlin.backend.konan.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.LateinitLowering
import org.jetbrains.kotlin.backend.konan.lower.VarargInjectionLowering
import org.jetbrains.kotlin.backend.konan.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.checkDeclarationParents
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.replaceUnboundSymbols

internal class KonanLower(val context: Context, val parentPhaser: PhaseManager) {

    fun lower() {
        val irModule = context.irModule!!

        // Phases to run against whole module.
        lowerModule(irModule, parentPhaser)

        // Phases to run against a file.
        irModule.files.forEach {
            lowerFile(it, PhaseManager(context, parentPhaser))
        }

        irModule.checkDeclarationParents()
    }

    private fun lowerModule(irModule: IrModuleFragment, phaser: PhaseManager) {
        phaser.phase(KonanPhase.REMOVE_EXPECT_DECLARATIONS) {
            irModule.files.forEach(ExpectDeclarationsRemoving(context)::lower)
        }

        phaser.phase(KonanPhase.TEST_PROCESSOR) {
            TestProcessor(context).process(irModule)
        }

        phaser.phase(KonanPhase.LOWER_BEFORE_INLINE) {
            irModule.files.forEach(PreInlineLowering(context)::lower)
        }

        // Inlining must be run before other phases.
        phaser.phase(KonanPhase.LOWER_INLINE) {
            FunctionInlining(context).inline(irModule)
        }

        phaser.phase(KonanPhase.LOWER_AFTER_INLINE) {
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        }

        phaser.phase(KonanPhase.LOWER_INTEROP_PART1) {
            irModule.files.forEach(InteropLoweringPart1(context)::lower)
        }

        irModule.patchDeclarationParents()

//        validateIrModule(context, irModule) // Temporarily disabled until moving to new IR finished.
    }

    private fun lowerFile(irFile: IrFile, phaser: PhaseManager) {
        phaser.phase(KonanPhase.LOWER_LATEINIT) {
            LateinitLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_STRING_CONCAT) {
            StringConcatenationLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DATA_CLASSES) {
            DataClassOperatorsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_FOR_LOOPS) {
            ForLoopsLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_ENUMS) {
            EnumClassLowering(context).run(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INITIALIZERS) {
            InitializersLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_SHARED_VARIABLES) {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DELEGATION) {
            PropertyDelegationLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_CALLABLES) {
            CallableReferenceLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_LOCAL_FUNCTIONS) {
            LocalDeclarationsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TAILREC) {
            TailrecLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_FINALLY) {
            FinallyBlocksLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DEFAULT_PARAMETER_EXTENT) {
            DefaultArgumentStubGenerator(context, skipInlineMethods = false).runOnFilePostfix(irFile)
            KonanDefaultParameterInjector(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_BUILTIN_OPERATORS) {
            BuiltinOperatorLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INNER_CLASSES) {
            InnerClassLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INTEROP_PART2) {
            InteropLoweringPart2(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_VARARG) {
            VarargInjectionLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_COMPILE_TIME_EVAL) {
            CompileTimeEvaluateLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_COROUTINES) {
            SuspendFunctionsLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TYPE_OPERATORS) {
            TypeOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.BRIDGES_BUILDING) {
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        }
        phaser.phase(KonanPhase.AUTOBOX) {
            // validateIrFile(context, irFile) // Temporarily disabled until moving to new IR finished.
            Autoboxing(context).lower(irFile)
        }
        phaser.phase(KonanPhase.RETURNS_INSERTION) {
            ReturnsInsertionLowering(context).lower(irFile)
        }
    }

}
