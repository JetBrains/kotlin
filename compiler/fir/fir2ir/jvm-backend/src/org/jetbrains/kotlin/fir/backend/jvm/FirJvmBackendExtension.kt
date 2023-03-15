/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizationResult
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.JvmAbiStability
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.org.objectweb.asm.Type

class FirJvmBackendExtension(
    private val components: Fir2IrComponents,
    private val irActualizationResult: IrActualizationResult?
) : JvmBackendExtension {
    override fun createSerializer(
        context: JvmBackendContext,
        klass: IrClass,
        type: Type,
        bindings: JvmSerializationBindings,
        parentSerializer: MetadataSerializer?
    ): MetadataSerializer {
        return makeFirMetadataSerializerForIrClass(
            components.session,
            context,
            klass,
            bindings,
            components,
            parentSerializer,
            irActualizationResult
        )
    }

    override fun generateMetadataExtraFlags(abiStability: JvmAbiStability?): Int =
        JvmAnnotationNames.METADATA_JVM_IR_FLAG or
                JvmAnnotationNames.METADATA_FIR_FLAG or
                (if (abiStability == JvmAbiStability.STABLE) JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG else 0)
}
