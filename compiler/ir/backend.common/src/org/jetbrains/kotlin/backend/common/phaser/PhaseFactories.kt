/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

annotation class PhaseDescription(
    val name: String,
    val description: String,
    val prerequisite: Array<KClass<out FileLoweringPass>> = [],
)

fun <Context : CommonBackendContext> createFilePhases(
    vararg phases: (Context) -> FileLoweringPass
): List<SimpleNamedCompilerPhase<Context, IrFile, IrFile>> {
    val createdPhases = hashSetOf<Class<out FileLoweringPass>>()
    return phases.map { phase ->
        val loweringClass = phase.extractReturnTypeArgument()
        createdPhases.add(loweringClass)
        createFilePhase(loweringClass, createdPhases, phase)
    }
}

fun <Context : CommonBackendContext> createModulePhases(
    vararg phases: (Context) -> ModuleLoweringPass
): List<SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>> {
    val createdPhases = hashSetOf<Class<out ModuleLoweringPass>>()
    return phases.map { phase ->
        val loweringClass = phase.extractReturnTypeArgument()
        createdPhases.add(loweringClass)
        createModulePhase(loweringClass, createdPhases, phase)
    }
}

private inline fun <ReturnType, reified FunctionType : Function<ReturnType>>
        FunctionType.extractReturnTypeArgument(): Class<out ReturnType> {
    // Using Java reflection to extract the generic type argument from the function type.
    // Note that we're not using kotlin-reflect because its initialization has some overhead.
    val functionType = javaClass.genericInterfaces.singleOrNull {
        it is ParameterizedType && it.rawType == FunctionType::class.java
    } ?: error("Supertype ${FunctionType::class.java} is not found: " + javaClass.genericInterfaces.toList())
    val returnTypeClass = (functionType as ParameterizedType).actualTypeArguments.last()
    @Suppress("UNCHECKED_CAST")
    return returnTypeClass as Class<out ReturnType>
}

private fun <Context : CommonBackendContext> createFilePhase(
    loweringClass: Class<*>,
    previouslyCreatedPhases: Set<Class<out FileLoweringPass>>,
    createLoweringPass: (Context) -> FileLoweringPass,
): SimpleNamedCompilerPhase<Context, IrFile, IrFile> {
    val annotation = loadAnnotationAndCheckPrerequisites(loweringClass, previouslyCreatedPhases)

    return createSimpleNamedCompilerPhase(
        name = annotation.name,
        description = annotation.description,
        preactions = defaultConditions,
        postactions = defaultConditions,
        prerequisite = emptySet(),
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            createLoweringPass(context).lower(irFile)
            irFile
        },
    )
}

private fun <Context : CommonBackendContext> createModulePhase(
    loweringClass: Class<*>,
    previouslyCreatedPhases: Set<Class<out ModuleLoweringPass>>,
    createLoweringPass: (Context) -> ModuleLoweringPass,
): SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> {
    val annotation = loadAnnotationAndCheckPrerequisites(loweringClass, previouslyCreatedPhases)

    return makeIrModulePhase(createLoweringPass, annotation.name, annotation.description)
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
