/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.metadata.DescriptorMetadataSerializer
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.JvmAbiStability
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.org.objectweb.asm.Type

interface JvmBackendExtension {
    fun createSerializer(
        context: JvmBackendContext, klass: IrClass, type: Type, bindings: JvmSerializationBindings, parentSerializer: MetadataSerializer?
    ): MetadataSerializer

    object Default : JvmBackendExtension {
        override fun createSerializer(
            context: JvmBackendContext,
            klass: IrClass,
            type: Type,
            bindings: JvmSerializationBindings,
            parentSerializer: MetadataSerializer?
        ): MetadataSerializer {
            return DescriptorMetadataSerializer(context, klass, type, bindings, parentSerializer)
        }

        fun generateMetadataExtraFlags(abiStability: JvmAbiStability?): Int =
            JvmAnnotationNames.METADATA_JVM_IR_FLAG or
                    (if (abiStability != JvmAbiStability.UNSTABLE) JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG else 0)
    }
}
