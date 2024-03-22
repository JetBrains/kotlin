/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.render

@PhaseDescription(
    name = "CodegenRegular",
    description = "Code generation of regular classes"
)
private class CodegenRegular(context: JvmBackendContext) : FileCodegen(context, generateMultifileFacade = false)

@PhaseDescription(
    name = "CodegenMultifileFacades",
    description = "Code generation of multifile facades"
)
private class CodegenMultifileFacades(context: JvmBackendContext) : FileCodegen(context, generateMultifileFacade = true)

private abstract class FileCodegen(
    private val context: JvmBackendContext, private val generateMultifileFacade: Boolean,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val isMultifileFacade = irFile.fileEntry is MultifileFacadeFileEntry
        if (isMultifileFacade == generateMultifileFacade) {
            for (loweredClass in irFile.declarations) {
                if (loweredClass !is IrClass) {
                    throw AssertionError("File-level declaration should be IrClass after JvmLower, got: " + loweredClass.render())
                }
                ClassCodegen.getOrCreate(loweredClass, context).generate()
            }
        }
    }
}

@PhaseDescription(
    name = "GenerateAdditionalClasses",
    description = "Generate additional classes that were requested during codegen",
)
private class GenerateAdditionalClassesPhase(private val context: JvmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        context.enumEntriesIntrinsicMappingsCache.generateMappingsClasses()
    }
}

// Generate multifile facades first, to compute and store JVM signatures of const properties which are later used
// when serializing metadata in the multifile parts.
// TODO: consider dividing codegen itself into separate phases (bytecode generation, metadata serialization) to avoid this
internal val jvmCodegenPhases = SameTypeNamedCompilerPhase(
    name = "Codegen",
    description = "Code generation",
    nlevels = 1,
    lower = performByIrFile(
        name = "CodegenByIrFileMultifileFacades",
        description = "Code generation by IrFile, multifile facades",
        copyBeforeLowering = false,
        lower = createFilePhases(::CodegenMultifileFacades)
    ) then performByIrFile(
        name = "CodegenByIrFileRegular",
        description = "Code generation by IrFile, regular files",
        copyBeforeLowering = false,
        lower = createFilePhases(::CodegenRegular)
    ) then createModulePhases(::GenerateAdditionalClassesPhase).single()
)

// This property is needed to avoid dependencies from "leaf" modules (cli, tests-common-new) on backend.jvm:lower.
// It's used to create PhaseConfig and is the only thing needed from lowerings in the leaf modules.
val jvmPhases: SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment>
    get() = jvmLoweringPhases
