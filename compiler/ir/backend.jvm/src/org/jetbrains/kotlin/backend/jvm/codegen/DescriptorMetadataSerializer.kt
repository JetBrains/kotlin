/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class DescriptorMetadataSerializer(
    private val irClass: IrClass,
    private val context: JvmBackendContext,
    private val localSerializationBindings: JvmSerializationBindings,
    parent: ClassMetadataSerializer?
) : ClassMetadataSerializer {
    private val serializerExtension = JvmSerializerExtension(localSerializationBindings, context.state, context.typeMapper)
    private val serializer: DescriptorSerializer? =
        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> DescriptorSerializer.create(
                metadata.descriptor, serializerExtension, (parent as? DescriptorMetadataSerializer)?.serializer
            )
            is MetadataSource.File -> DescriptorSerializer.createTopLevel(serializerExtension)
            is MetadataSource.Function -> DescriptorSerializer.createForLambda(serializerExtension)
            else -> null
        }

    override val isSynthetic: Boolean
        get() = irClass.metadata !is MetadataSource.Class

    override val stringTable: JvmStringTable
        get() = serializerExtension.stringTable

    override fun generateMetadataProto(type: Type): MessageLite? =
        when (val metadata = irClass.metadata) {
            is MetadataSource.Function ->
                serializer!!.functionProto(createFreeFakeLambdaDescriptor(metadata.descriptor, context.state.typeApproximator))?.build()
            is MetadataSource.Class -> serializer!!.classProto(metadata.descriptor).build()
            is MetadataSource.File ->
                serializer!!.packagePartProto(irClass.getPackageFragment()!!.fqName, metadata.descriptors).also {
                    serializerExtension.serializeJvmPackage(it, type)
                }.build()
            else -> null
        }

    override fun bindMethodMetadata(method: IrFunction, signature: Method) {
        when (val metadata = method.metadata) {
            is MetadataSource.Property -> {
                // We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
                // moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
                // TODO: fix origin somehow, because otherwise $annotations methods in interfaces also don't have ACC_SYNTHETIC
                assert(method.name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) { method.dump() }

                context.state.globalSerializationBindings.put(
                    JvmSerializationBindings.SYNTHETIC_METHOD_FOR_PROPERTY, metadata.descriptor, signature
                )
            }
            is MetadataSource.Function -> {
                localSerializationBindings.put(JvmSerializationBindings.METHOD_FOR_FUNCTION, metadata.descriptor, signature)
            }
            null -> {
            }
            else -> error("Incorrect metadata source $metadata for:\n${method.dump()}")
        }
    }

    override fun bindFieldMetadata(field: IrField, fieldType: Type, fieldName: String) {
        val descriptor = (field.metadata as? MetadataSource.Property)?.descriptor
        if (descriptor != null) {
            context.state.globalSerializationBindings.put(JvmSerializationBindings.FIELD_FOR_PROPERTY, descriptor, fieldType to fieldName)
        }
    }
}
