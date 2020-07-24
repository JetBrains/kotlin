/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class DescriptorBasedClassCodegen internal constructor(
    irClass: IrClass,
    context: JvmBackendContext,
    parentFunction: IrFunction?,
) : ClassCodegen(irClass, context, parentFunction) {

    private val serializerExtension = JvmSerializerExtension(visitor.serializationBindings, state, typeMapper)
    private val serializer: DescriptorSerializer? =
        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> DescriptorSerializer.create(
                metadata.descriptor, serializerExtension, (parentClassCodegen as? DescriptorBasedClassCodegen)?.serializer
            )
            is MetadataSource.File -> DescriptorSerializer.createTopLevel(serializerExtension)
            is MetadataSource.Function -> DescriptorSerializer.createForLambda(serializerExtension)
            else -> null
        }

    override fun generateKotlinMetadataAnnotation() {

        val localDelegatedProperties = (irClass.attributeOwnerId as? IrClass)?.let(context.localDelegatedProperties::get)
        if (localDelegatedProperties != null && localDelegatedProperties.isNotEmpty()) {
            state.bindingTrace.record(
                CodegenBinding.DELEGATED_PROPERTIES_WITH_METADATA,
                type,
                localDelegatedProperties.mapNotNull { (it.owner.metadata as? MetadataSource.LocalDelegatedProperty)?.descriptor }
            )
        }

        // TODO: if `-Xmultifile-parts-inherit` is enabled, write the corresponding flag for parts and facades to [Metadata.extraInt].
        var extraFlags = JvmAnnotationNames.METADATA_JVM_IR_FLAG
        if (state.isIrWithStableAbi) {
            extraFlags += JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG
        }

        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> {
                val classProto = serializer!!.classProto(metadata.descriptor).build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.CLASS, extraFlags) {
                    AsmUtil.writeAnnotationData(it, serializer, classProto)
                }

                assert(irClass !in context.classNameOverride) {
                    "JvmPackageName is not supported for classes: ${irClass.render()}"
                }
            }
            is MetadataSource.File -> {
                val packageFqName = irClass.getPackageFragment()!!.fqName
                val packageProto = serializer!!.packagePartProto(packageFqName, metadata.descriptors)

                serializerExtension.serializeJvmPackage(packageProto, type)

                val facadeClassName = context.multifileFacadeForPart[irClass.attributeOwnerId]
                val kind = if (facadeClassName != null) KotlinClassHeader.Kind.MULTIFILE_CLASS_PART else KotlinClassHeader.Kind.FILE_FACADE
                writeKotlinMetadata(visitor, state, kind, extraFlags) { av ->
                    AsmUtil.writeAnnotationData(av, serializer, packageProto.build())

                    if (facadeClassName != null) {
                        av.visit(JvmAnnotationNames.METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME, facadeClassName.internalName)
                    }

                    if (irClass in context.classNameOverride) {
                        av.visit(JvmAnnotationNames.METADATA_PACKAGE_NAME_FIELD_NAME, irClass.fqNameWhenAvailable!!.parent().asString())
                    }
                }
            }
            is MetadataSource.Function -> {
                val fakeDescriptor = createFreeFakeLambdaDescriptor(metadata.descriptor, state.typeApproximator)
                val functionProto = serializer!!.functionProto(fakeDescriptor)?.build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, extraFlags) {
                    if (functionProto != null) {
                        AsmUtil.writeAnnotationData(it, serializer, functionProto)
                    }
                }
            }
            else -> {
                val entry = irClass.fileParent.fileEntry
                if (entry is MultifileFacadeFileEntry) {
                    val partInternalNames = entry.partFiles.mapNotNull { partFile ->
                        val fileClass = partFile.declarations.singleOrNull { it.isFileClass } as IrClass?
                        if (fileClass != null) typeMapper.mapClass(fileClass).internalName else null
                    }
                    MultifileClassCodegenImpl.writeMetadata(
                        visitor, state, extraFlags, partInternalNames, type, irClass.fqNameWhenAvailable!!.parent()
                    )
                } else {
                    writeSyntheticClassMetadata(visitor, state)
                }
            }
        }
    }

    override fun bindMethodMetadata(method: IrFunction, signature: Method) {
        when (val metadata = method.metadata) {
            is MetadataSource.Property -> {
                // We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
                // moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
                // TODO: fix origin somehow, because otherwise $annotations methods in interfaces also don't have ACC_SYNTHETIC
                assert(method.name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) { method.dump() }

                state.globalSerializationBindings.put(
                    JvmSerializationBindings.SYNTHETIC_METHOD_FOR_PROPERTY, metadata.descriptor, signature
                )
            }
            is MetadataSource.Function -> {
                visitor.serializationBindings.put(JvmSerializationBindings.METHOD_FOR_FUNCTION, metadata.descriptor, signature)
            }
            null -> {
            }
            else -> error("Incorrect metadata source $metadata for:\n${method.dump()}")
        }
    }

    override fun bindFieldMetadata(field: IrField, fieldType: Type, fieldName: String) {
        val descriptor = (field.metadata as? MetadataSource.Property)?.descriptor
        if (descriptor != null) {
            state.globalSerializationBindings.put(JvmSerializationBindings.FIELD_FOR_PROPERTY, descriptor, fieldType to fieldName)
        }
    }
}
