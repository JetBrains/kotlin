/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.config.phaser.ActionState
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.lang.reflect.ParameterizedType

annotation class PhaseDescription(val name: String)

fun <Context : LoweringContext> createFilePhases(
    vararg phases: ((Context) -> FileLoweringPass)?
): List<NamedCompilerPhase<Context, IrFile, IrFile>> {
    return phases.filterNotNull().map { phase ->
        FileLoweringPhase(phase.extractReturnTypeArgument(), phase)
    }
}

fun <Context : LoweringContext> createModulePhases(
    vararg phases: ((Context) -> ModuleLoweringPass)?
): List<NamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>> {
    return phases.filterNotNull().map { phase ->
        ModuleLoweringPhase(phase.extractReturnTypeArgument(), phase)
    }
}

private inline fun <ReturnType, reified FunctionType : Function<ReturnType>>
        FunctionType.extractReturnTypeArgument(): Class<out ReturnType> {
    // Using Java reflection to extract the generic type argument from the function type.
    // Note that we're not using kotlin-reflect because its initialization has some overhead.
    val functionType = javaClass.genericInterfaces.singleOrNull {
        it is ParameterizedType && it.rawType == FunctionType::class.java
    } ?: error("Supertype ${FunctionType::class.java} is not found: " + javaClass.genericInterfaces.toList())
    val returnType = (functionType as ParameterizedType).actualTypeArguments.last()
    @Suppress("UNCHECKED_CAST")
    return when (returnType) {
        is Class<*> -> returnType
        is ParameterizedType -> returnType.rawType
        else -> error("Unexpected return type ${returnType.typeName}")
    } as Class<out ReturnType>
}

private class FileLoweringPhase<Context : LoweringContext>(
    loweringClass: Class<out FileLoweringPass>,
    createLoweringPass: (Context) -> FileLoweringPass,
) : LoweringPhase<Context, IrFile, FileLoweringPass>(loweringClass, createLoweringPass) {
    override fun phaseBody(context: Context, input: IrFile): IrFile {
        createLoweringPass(context).lower(input)
        return input
    }
}

private class ModuleLoweringPhase<Context : LoweringContext>(
    loweringClass: Class<out ModuleLoweringPass>,
    createLoweringPass: (Context) -> ModuleLoweringPass,
) : LoweringPhase<Context, IrModuleFragment, ModuleLoweringPass>(loweringClass, createLoweringPass) {
    override fun phaseBody(context: Context, input: IrModuleFragment): IrModuleFragment {
        createLoweringPass(context).lower(input)
        return input
    }
}

abstract class LoweringPhase<Context : LoweringContext, Input : IrElement, Pass : ModuleLoweringPass>(
    val loweringClass: Class<out Pass>,
    protected val createLoweringPass: (Context) -> Pass,
) : NamedCompilerPhase<Context, Input, Input>(
    loadPhaseName(loweringClass),
    preactions = DEFAULT_IR_ACTIONS,
    postactions = DEFAULT_IR_ACTIONS.map { f ->
        fun(actionState: ActionState, data: Pair<Input, Input>, context: Context) = f(actionState, data.second, context)
    }.toSet(),
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Input = input
}

private fun loadPhaseName(loweringClass: Class<*>): String =
    loweringClass.getDeclaredAnnotation(PhaseDescription::class.java)?.name
        ?: error("Lowering phase is missing the @PhaseDescription annotation: ${loweringClass.name}")
