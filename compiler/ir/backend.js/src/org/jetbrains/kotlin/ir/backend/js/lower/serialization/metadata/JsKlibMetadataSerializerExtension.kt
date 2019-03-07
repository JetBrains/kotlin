/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.types.FlexibleType

class JsKlibMetadataSerializerExtension(
    private val fileRegistry: JsKlibMetadataFileRegistry,
    private val languageVersionSettings: LanguageVersionSettings,
    override val metadataVersion: BinaryVersion,
    val declarationTableHandler: ((DeclarationDescriptor) -> JsKlibMetadataProtoBuf.DescriptorUniqId?)
) : KotlinSerializerExtensionBase(JsKlibMetadataSerializerProtocol) {
    override val stringTable = JsKlibMetadataStringTable()

    override fun serializeFlexibleType(flexibleType: FlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(DynamicTypeDeserializer.id)
    }

    private fun uniqId(descriptor: DeclarationDescriptor): JsKlibMetadataProtoBuf.DescriptorUniqId? {
//        val index = declarationTable.descriptorTable.get(descriptor)
//        return index?.let { newDescriptorUniqId(it) }
        return declarationTableHandler(descriptor)
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        uniqId(typeParameter)?.let { proto.setExtension(JsKlibMetadataProtoBuf.typeParamUniqId, it) }
        super.serializeTypeParameter(typeParameter, proto)
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        uniqId(descriptor)?.let { proto.setExtension(JsKlibMetadataProtoBuf.valueParamUniqId, it) }
        super.serializeValueParameter(descriptor, proto)
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
        uniqId(descriptor)?.let { proto.setExtension(JsKlibMetadataProtoBuf.enumEntryUniqId, it) }
        super.serializeEnumEntry(descriptor, proto)
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder,
                                      childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(JsKlibMetadataProtoBuf.constructorUniqId, it) }
        super.serializeConstructor(descriptor, proto, childSerializer)
    }

    override fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(JsKlibMetadataProtoBuf.classUniqId, it) }
        val id = getFileId(descriptor)
        if (id != null) {
            proto.setExtension(JsKlibMetadataProtoBuf.classContainingFileId, id)
        }
        super.serializeClass(descriptor, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(JsKlibMetadataProtoBuf.propertyUniqId, it) }
        val id = getFileId(descriptor)
        if (id != null) {
            proto.setExtension(JsKlibMetadataProtoBuf.propertyContainingFileId, id)
        }
        super.serializeProperty(descriptor, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeFunction(descriptor: FunctionDescriptor,
                                   proto: ProtoBuf.Function.Builder,
                                   childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(JsKlibMetadataProtoBuf.functionUniqId, it) }
        val id = getFileId(descriptor)
        if (id != null) {
            proto.setExtension(JsKlibMetadataProtoBuf.functionContainingFileId, id)
        }
        super.serializeFunction(descriptor, proto, childSerializer)
    }

    private fun getFileId(descriptor: DeclarationDescriptor): Int? {
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor) || descriptor !is DeclarationDescriptorWithSource) return null

        val fileId = descriptor.extractFileId()
        if (fileId != null) {
            (descriptor.containingDeclaration as? JsKlibMetadataPackageFragment)?.let { packageFragment ->
                return fileRegistry.lookup(KotlinDeserializedFileMetadata(packageFragment, fileId))
            }
        }

        val file = descriptor.source.containingFile as? PsiSourceFile ?: return null

        val psiFile = file.psiFile
        return (psiFile as? KtFile)?.let { fileRegistry.lookup(KotlinPsiFileMetadata(it)) }
    }

    override fun releaseCoroutines() = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
}
