/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.render

private val notifyCodegenStartPhase = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    op = { context, _ -> context.notifyCodegenStart() },
    name = "NotifyCodegenStart",
    description = "Notify time measuring subsystem that code generation is being started",
)

private fun codegenPhase(generateMultifileFacade: Boolean): NamedCompilerPhase<JvmBackendContext, IrModuleFragment> {
    val suffix = if (generateMultifileFacade) "MultifileFacades" else "Regular"
    val descriptionSuffix = if (generateMultifileFacade) ", multifile facades" else ", regular files"
    return performByIrFile(
        name = "CodegenByIrFile$suffix",
        description = "Code generation by IrFile$descriptionSuffix",
        copyBeforeLowering = false,
        lower = listOf(
            makeIrFilePhase(
                { context ->
                    object : FileLoweringPass {
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
                },
                name = "Codegen$suffix",
                description = "Code generation"
            )
        )
    )
}

// Generate multifile facades first, to compute and store JVM signatures of const properties which are later used
// when serializing metadata in the multifile parts.
// TODO: consider dividing codegen itself into separate phases (bytecode generation, metadata serialization) to avoid this
private val jvmCodegenPhases = NamedCompilerPhase(
    name = "Codegen",
    description = "Code generation",
    nlevels = 1,
    lower = codegenPhase(generateMultifileFacade = true) then
            codegenPhase(generateMultifileFacade = false)
)

val jvmPhases = NamedCompilerPhase(
    name = "IrBackend",
    description = "IR Backend for JVM",
    nlevels = 1,
    actions = setOf(defaultDumper, validationAction),
    lower = jvmLoweringPhases then
            notifyCodegenStartPhase then
            jvmCodegenPhases
)
