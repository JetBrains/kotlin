/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.types.FlexibleType

class KlibMetadataSerializerExtension(
    private val languageVersionSettings: LanguageVersionSettings,
    override val metadataVersion: BinaryVersion,
    val declarationTableHandler: (DeclarationDescriptor) -> KlibMetadataProtoBuf.DescriptorUniqId?,
    override val stringTable: StringTableImpl
) : KotlinSerializerExtensionBase(KlibMetadataSerializerProtocol) {
    override fun shouldUseTypeTable(): Boolean = true

    private fun descriptorFileId(descriptor: DeclarationDescriptorWithSource): Int? {
        val fileName = descriptor.source.containingFile.name ?: return null
        return stringTable.getStringIndex(fileName)
    }

    override fun serializeFlexibleType(flexibleType: FlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(DynamicTypeDeserializer.id)
    }

    private fun uniqId(descriptor: DeclarationDescriptor): KlibMetadataProtoBuf.DescriptorUniqId? {
        return declarationTableHandler(descriptor)
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        uniqId(typeParameter)?.let { proto.setExtension(KlibMetadataProtoBuf.typeParamUniqId, it) }
        super.serializeTypeParameter(typeParameter, proto)
    }

    override fun serializeTypeAlias(typeAlias: TypeAliasDescriptor, proto: ProtoBuf.TypeAlias.Builder) {
        uniqId(typeAlias)?.let { proto.setExtension(KlibMetadataProtoBuf.typeAliasUniqId, it) }
        super.serializeTypeAlias(typeAlias, proto)
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
        uniqId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.enumEntryUniqId, it) }
        super.serializeEnumEntry(descriptor, proto)
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder,
                                      childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.constructorUniqId, it) }
        super.serializeConstructor(descriptor, proto, childSerializer)
    }

    override fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.classUniqId, it) }
        descriptorFileId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.classFile, it) }
        super.serializeClass(descriptor, proto, versionRequirementTable, childSerializer)
        childSerializer.typeTable.serialize()?.let { proto.mergeTypeTable(it) }
    }

    override fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.propertyUniqId, it) }
        descriptorFileId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.propertyFile, it) }
        super.serializeProperty(descriptor, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        uniqId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.functionUniqId, it) }
        descriptorFileId(descriptor)?.let { proto.setExtension(KlibMetadataProtoBuf.functionFile, it) }
        super.serializeFunction(descriptor, proto, versionRequirementTable, childSerializer)
    }

    override fun releaseCoroutines() =
        languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
}
