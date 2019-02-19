/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.konan.KonanSerializerProtocol
import org.jetbrains.kotlin.serialization.konan.SourceFileMap
import org.jetbrains.kotlin.types.KotlinType

internal class KonanSerializerExtension(val context: Context, override val metadataVersion: BinaryVersion,
                                        val sourceFileMap: SourceFileMap, val declarationTable: DeclarationTable) :
        KotlinSerializerExtensionBase(KonanSerializerProtocol) {

    override val stringTable = KonanStringTable()
    override fun shouldUseTypeTable(): Boolean = true

    fun uniqId(descriptor: DeclarationDescriptor): KonanProtoBuf.DescriptorUniqId? {
        val index = declarationTable.descriptorTable.get(descriptor)
        return index?.let { newDescriptorUniqId(it) }
    }

    override fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
        // TODO: For debugging purpose we store the textual
        // representation of serialized types.
        // To be removed.
        proto.setExtension(KonanProtoBuf.typeText, type.toString())

        super.serializeType(type, proto)
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        uniqId(typeParameter) ?.let { proto.setExtension(KonanProtoBuf.typeParamUniqId, it) }
        super.serializeTypeParameter(typeParameter, proto)
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        uniqId(descriptor) ?. let { proto.setExtension(KonanProtoBuf.valueParamUniqId, it) }
        super.serializeValueParameter(descriptor, proto)
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
        uniqId(descriptor) ?.let { proto.setExtension(KonanProtoBuf.enumEntryUniqId, it) }
        // Serialization doesn't preserve enum entry order, so we need to serialize ordinal.
        super.serializeEnumEntry(descriptor, proto)
    }

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder,
                                versionRequirementTable: MutableVersionRequirementTable,
                                childSerializer: DescriptorSerializer) {
        uniqId(descriptor) ?. let { proto.setExtension(KonanProtoBuf.classUniqId, it) }
        super.serializeClass(descriptor, proto, versionRequirementTable, childSerializer)
        childSerializer.typeTable.serialize()?.let { proto.mergeTypeTable(it) }
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder,
                                      childSerializer: DescriptorSerializer) {
        uniqId(descriptor) ?. let { proto.setExtension(KonanProtoBuf.constructorUniqId, it) }
        super.serializeConstructor(descriptor, proto, childSerializer)
    }


    override fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder,
                                   childSerializer: DescriptorSerializer) {
        proto.setExtension(KonanProtoBuf.functionFile, sourceFileMap.assign(descriptor.source.containingFile))
        uniqId(descriptor) ?. let { proto.setExtension(KonanProtoBuf.functionUniqId, it) }
        super.serializeFunction(descriptor, proto, childSerializer)
    }

    override fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder,
                                   versionRequirementTable: MutableVersionRequirementTable,
                                   childSerializer: DescriptorSerializer) {
        proto.setExtension(KonanProtoBuf.propertyFile, sourceFileMap.assign(descriptor.source.containingFile))
        uniqId(descriptor) ?.let { proto.setExtension(KonanProtoBuf.propertyUniqId, it) }
        proto.setExtension(KonanProtoBuf.hasBackingField,
            context.ir.propertiesWithBackingFields.contains(descriptor))

        super.serializeProperty(descriptor, proto, versionRequirementTable, childSerializer)
    }

    override fun releaseCoroutines(): Boolean =
            context.config.configuration.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)

}
