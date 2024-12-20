/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

annotation class PhaseDescription(
    val name: String,
    val prerequisite: Array<KClass<out FileLoweringPass>> = [],
)

fun <Context : LoweringContext> createFilePhases(
    vararg phases: ((Context) -> FileLoweringPass)?
): List<SimpleNamedCompilerPhase<Context, IrFile, IrFile>> {
    val createdPhases = hashSetOf<Class<out FileLoweringPass>>()
    return phases.filterNotNull().map { phase ->
        val loweringClass = phase.extractReturnTypeArgument()
        createdPhases.add(loweringClass)
        createFilePhase(loweringClass, createdPhases, phase)
    }
}

fun <Context : LoweringContext> createModulePhases(
    vararg phases: ((Context) -> ModuleLoweringPass)?
): List<SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>> {
    val createdPhases = hashSetOf<Class<out ModuleLoweringPass>>()
    return phases.filterNotNull().map { phase ->
        val loweringClass = phase.extractReturnTypeArgument()
        createdPhases.add(loweringClass)
        createModulePhase(loweringClass, createdPhases, phase)
    }
}

fun <Context : LoweringContext> buildModuleLoweringsPhase(
    vararg phases: ((Context) -> ModuleLoweringPass)?
): CompilerPhase<Context, IrModuleFragment, IrModuleFragment> =
    createModulePhases(*phases)
        .fold(noopPhase(), CompilerPhase<Context, IrModuleFragment, IrModuleFragment>::then)

private fun <Context : LoweringContext, T> noopPhase(): CompilerPhase<Context, T, T> =
    object : CompilerPhase<Context, T, T> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<T>, context: Context, input: T): T = input
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

private fun <Context : LoweringContext> createFilePhase(
    loweringClass: Class<*>,
    previouslyCreatedPhases: Set<Class<out FileLoweringPass>>,
    createLoweringPass: (Context) -> FileLoweringPass,
): SimpleNamedCompilerPhase<Context, IrFile, IrFile> {
    val annotation = loadAnnotationAndCheckPrerequisites(loweringClass, previouslyCreatedPhases)

    return createSimpleNamedCompilerPhase(
        name = annotation.name,
        preactions = DEFAULT_IR_ACTIONS,
        postactions = DEFAULT_IR_ACTIONS,
        prerequisite = emptySet(),
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            createLoweringPass(context).lower(irFile)
            irFile
        },
    )
}

private fun <Context : LoweringContext> createModulePhase(
    loweringClass: Class<*>,
    previouslyCreatedPhases: Set<Class<out ModuleLoweringPass>>,
    createLoweringPass: (Context) -> ModuleLoweringPass,
): SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> {
    val annotation = loadAnnotationAndCheckPrerequisites(loweringClass, previouslyCreatedPhases)

    return makeIrModulePhase(createLoweringPass, annotation.name)
}

private fun loadAnnotationAndCheckPrerequisites(
    loweringClass: Class<*>,
    previouslyCreatedPhases: Set<Class<*>>,
): PhaseDescription {
    val annotation = loweringClass.getDeclaredAnnotation(PhaseDescription::class.java)
        ?: error("Lowering phase is missing the @PhaseDescription annotation: ${loweringClass.name}")

    for (prerequisite in annotation.prerequisite) {
        if (prerequisite.java !in previouslyCreatedPhases) {
            error("Prerequisite ${prerequisite.java.simpleName} is not satisfied for ${loweringClass.simpleName}")
        }
    }
    return annotation
}
