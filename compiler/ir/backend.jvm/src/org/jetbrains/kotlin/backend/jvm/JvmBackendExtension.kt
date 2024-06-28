/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.metadata.DescriptorMetadataSerializer
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.codegen.JvmOptionalAnnotationSerializerExtension
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.JvmAbiStability
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.StringTableImpl
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

        override fun createModuleMetadataSerializer(context: JvmBackendContext) = object : ModuleMetadataSerializer {
            override fun serializeOptionalAnnotationClass(metadata: MetadataSource.Class, stringTable: StringTableImpl): ProtoBuf.Class {
                require(metadata is DescriptorMetadataSource.Class)
                return DescriptorSerializer.createTopLevel(
                    JvmOptionalAnnotationSerializerExtension(stringTable), context.state.config.languageVersionSettings,
                ).classProto(metadata.descriptor).build()
            }
        }


        fun generateMetadataExtraFlags(abiStability: JvmAbiStability?): Int =
            JvmAnnotationNames.METADATA_JVM_IR_FLAG or
                    (if (abiStability != JvmAbiStability.UNSTABLE) JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG else 0)
    }

    fun createModuleMetadataSerializer(context: JvmBackendContext): ModuleMetadataSerializer
}

interface ModuleMetadataSerializer {
    fun serializeOptionalAnnotationClass(metadata: MetadataSource.Class, stringTable: StringTableImpl): ProtoBuf.Class
}