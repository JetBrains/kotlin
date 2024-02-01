/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.metadata

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.createFreeFakeLambdaDescriptor
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class DescriptorMetadataSerializer(
    private val context: JvmBackendContext,
    private val irClass: IrClass,
    private val type: Type,
    private val serializationBindings: JvmSerializationBindings,
    parent: MetadataSerializer?
) : MetadataSerializer {
    private val serializerExtension = JvmSerializerExtension(serializationBindings, context.state, context.defaultTypeMapper)
    private val serializer: DescriptorSerializer? = run {
        val languageVersionSettings = context.config.languageVersionSettings
        when (val metadata = irClass.metadata) {
            is DescriptorMetadataSource.Class -> DescriptorSerializer.create(
                metadata.descriptor, serializerExtension, (parent as? DescriptorMetadataSerializer)?.serializer,
                languageVersionSettings, context.state.project,
            )
            is DescriptorMetadataSource.Script -> DescriptorSerializer.create(
                metadata.descriptor, serializerExtension, (parent as? DescriptorMetadataSerializer)?.serializer,
                languageVersionSettings, context.state.project,
            )
            is DescriptorMetadataSource.File -> DescriptorSerializer.createTopLevel(
                serializerExtension, languageVersionSettings,
                context.state.project
            )
            is DescriptorMetadataSource.Function -> DescriptorSerializer.createForLambda(serializerExtension, languageVersionSettings)
            else -> null
        }
    }

    override fun serialize(metadata: MetadataSource): Pair<MessageLite, JvmStringTable>? {
        val localDelegatedProperties = context.localDelegatedProperties[irClass.attributeOwnerId]
        if (localDelegatedProperties != null && localDelegatedProperties.isNotEmpty()) {
            context.state.bindingTrace.record(
                CodegenBinding.DELEGATED_PROPERTIES_WITH_METADATA,
                // key for local delegated properties metadata in interfaces depends on jvmDefaultMode
                if (irClass.isInterface && !context.config.jvmDefaultMode.isEnabled) context.defaultTypeMapper.mapClass(
                    context.cachedDeclarations.getDefaultImplsClass(irClass)
                ) else type,
                localDelegatedProperties.mapNotNull { (it.owner.metadata as? DescriptorMetadataSource.LocalDelegatedProperty)?.descriptor }
            )
        }
        val message = when (metadata) {
            is DescriptorMetadataSource.Class -> serializer!!.classProto(metadata.descriptor).build()
            is DescriptorMetadataSource.Script -> serializer!!.classProto(metadata.descriptor).build()
            is DescriptorMetadataSource.File ->
                serializer!!.packagePartProto(irClass.getPackageFragment().packageFqName, metadata.descriptors).apply {
                    serializerExtension.serializeJvmPackage(this, type)
                }.build()
            is DescriptorMetadataSource.Function -> {
                val withTypeParameters = createFreeFakeLambdaDescriptor(metadata.descriptor, context.state.typeApproximator)
                serializationBindings.get(JvmSerializationBindings.METHOD_FOR_FUNCTION, metadata.descriptor)?.let {
                    serializationBindings.put(JvmSerializationBindings.METHOD_FOR_FUNCTION, withTypeParameters, it)
                }
                serializer!!.functionProto(withTypeParameters)?.build()
            }
            else -> null
        } ?: return null
        return message to serializer!!.stringTable as JvmStringTable
    }

    override fun bindPropertyMetadata(metadata: MetadataSource.Property, signature: Method, origin: IrDeclarationOrigin) {
        val descriptor = (metadata as DescriptorMetadataSource.Property).descriptor
        val slice = when (origin) {
            JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS ->
                JvmSerializationBindings.SYNTHETIC_METHOD_FOR_PROPERTY
            IrDeclarationOrigin.PROPERTY_DELEGATE ->
                JvmSerializationBindings.DELEGATE_METHOD_FOR_PROPERTY
            else -> throw IllegalStateException("invalid origin $origin for property-related method $signature")
        }
        context.state.globalSerializationBindings.put(slice, descriptor, signature)
    }

    override fun bindMethodMetadata(metadata: MetadataSource.Function, signature: Method) {
        val descriptor = (metadata as DescriptorMetadataSource.Function).descriptor
        serializationBindings.put(JvmSerializationBindings.METHOD_FOR_FUNCTION, descriptor, signature)
    }

    override fun bindFieldMetadata(metadata: MetadataSource.Property, signature: Pair<Type, String>) {
        val descriptor = (metadata as DescriptorMetadataSource.Property).descriptor
        context.state.globalSerializationBindings.put(JvmSerializationBindings.FIELD_FOR_PROPERTY, descriptor, signature)
    }
}
