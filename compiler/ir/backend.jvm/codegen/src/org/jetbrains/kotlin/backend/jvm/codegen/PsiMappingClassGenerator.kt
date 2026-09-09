/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.codegen.PsiMappingMetadata
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

/**
 * Generates the synthetic per-module class annotated with `@kotlin.internal.PSIMappingMetadata` that carries the PSI <-> bytecode
 * mapping of all classes of the module collected in [JvmBackendContext.psiMappingEntries], see [PsiMappingMetadata].
 */
object PsiMappingClassGenerator {
    fun generate(context: JvmBackendContext, module: IrModuleFragment) {
        if (!context.config.generatePsiMetadata) return
        val entries = synchronized(context.psiMappingEntries) { context.psiMappingEntries.toList() }
        if (entries.isEmpty()) return

        val state = context.state
        val type = Type.getObjectType(PsiMappingMetadata.bearerClassInternalName(state.moduleName))
        val builder = state.factory.newVisitor(null, type, module.files.mapNotNull { it.getIoFile() })
        builder.defineClass(
            context.config.classFileVersion,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER or Opcodes.ACC_SYNTHETIC,
            type.internalName,
            null,
            "java/lang/Object",
            emptyArray(),
        )

        val annotation = builder.newAnnotation(PsiMappingMetadata.ANNOTATION_TYPE.descriptor, true)
        val parts = annotation.visitArray(PsiMappingMetadata.MAPPING_PARTS_ARGUMENT)
        for (part in PsiMappingMetadata.encode(entries)) {
            parts.visit(null, part)
        }
        parts.visitEnd()
        annotation.visitEnd()

        builder.done(false)
    }
}
