/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isOptionalAnnotationClass
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil

internal val processOptionalAnnotationsPhase = makeIrModulePhase(
    { context: JvmBackendContext ->
        if (context.config.useFir) ProcessOptionalAnnotationsFir(context) else ProcessOptionalAnnotationsDescriptors(context)
    },
    name = "ProcessOptionalAnnotations",
    description = "Record metadata of @OptionalExpectation-annotated classes to backend-specific storage, later written to .kotlin_module"
)

class ProcessOptionalAnnotationsDescriptors(private val context: JvmBackendContext) : ProcessOptionalAnnotations() {
    override fun IrClass.processClassFrontendSpecific() {
        val classMetadata = metadata

        require(classMetadata is DescriptorMetadataSource.Class?) { "IrClass has unexpected metadata: ${classMetadata!!::class.simpleName}" }

        if (classMetadata != null) {
            val descriptor = classMetadata.descriptor
            if (OptionalAnnotationUtil.shouldGenerateExpectClass(descriptor)) {
                context.state.factory.packagePartRegistry.optionalAnnotations += descriptor
                context.optionalAnnotations += classMetadata
            }
        }
    }
}

class ProcessOptionalAnnotationsFir(private val context: JvmBackendContext) : ProcessOptionalAnnotations() {
    override fun IrClass.processClassFrontendSpecific() {
        val classMetadata = metadata
        require(classMetadata is MetadataSource.Class?) { "IrClass has unexpected metadata: ${classMetadata!!::class.simpleName}" }

        if (classMetadata != null && isAnnotationClass && isExpect && hasAnnotation(OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME)) {
            context.optionalAnnotations += classMetadata
        }
    }
}

abstract class ProcessOptionalAnnotations : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        for (declaration in irFile.declarations) {
            if (declaration !is IrClass || !declaration.isOptionalAnnotationClass) continue
            declaration.registerOptionalAnnotations()
        }
    }

    abstract fun IrClass.processClassFrontendSpecific()

    private fun IrClass.registerOptionalAnnotations() {
        processClassFrontendSpecific()

        declarations.forEach {
            if (it is IrClass && it.isOptionalAnnotationClass) it.registerOptionalAnnotations()
        }
    }

}
