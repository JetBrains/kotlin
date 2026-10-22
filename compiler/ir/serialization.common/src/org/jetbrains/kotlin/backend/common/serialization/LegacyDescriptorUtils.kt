/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.library.metadata.DeserializedSourceFile
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.KlibMetadataDeserializedPackageFragment
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeAliasDescriptor

@Deprecated("Moved to the ':core:descriptors' module", level = DeprecationLevel.HIDDEN)
fun DeclarationDescriptor.findPackage(): PackageFragmentDescriptor = findPackage()

private fun sourceByIndex(descriptor: CallableMemberDescriptor, index: Int): SourceFile {
    val fragment = descriptor.findPackage() as KlibMetadataDeserializedPackageFragment
    val fileName = fragment.proto.strings.stringList[index]
    return DeserializedSourceFile(fileName, (fragment.source as KlibDeserializedContainerSource).klib)
}

@OptIn(K1Deprecation::class)
fun CallableMemberDescriptor.findSourceFile(): SourceFile {
    val source = this.source.containingFile
    if (source != SourceFile.NO_SOURCE_FILE)
        return source
    return when {
        this is DeserializedSimpleFunctionDescriptor && proto.hasExtension(KlibMetadataProtoBuf.functionFile) ->
            sourceByIndex(
                this, proto.getExtension(KlibMetadataProtoBuf.functionFile))
        this is DeserializedPropertyDescriptor && proto.hasExtension(KlibMetadataProtoBuf.propertyFile) ->
            sourceByIndex(
                this, proto.getExtension(KlibMetadataProtoBuf.propertyFile))
        else -> TODO()
    }
}

@OptIn(K1Deprecation::class)
fun DeclarationDescriptor.isDeserialized(): Boolean = when (this) {
    is DeserializedDescriptor -> true
    is EnumEntrySyntheticClassDescriptor -> containingDeclaration is DeserializedClassDescriptor
    is PropertyAccessorDescriptor -> correspondingProperty is DeserializedPropertyDescriptor
    else -> false
}

@OptIn(K1Deprecation::class)
fun DeclarationDescriptor.extractSerializedKdocString(): String? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtensionOrNull(KlibMetadataProtoBuf.classKdoc)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtensionOrNull(KlibMetadataProtoBuf.functionKdoc)
    is DeserializedPropertyDescriptor -> proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyKdoc)
    is DeserializedClassConstructorDescriptor -> proto.getExtensionOrNull(KlibMetadataProtoBuf.constructorKdoc)
    is DeserializedTypeAliasDescriptor -> proto.getExtensionOrNull(KlibMetadataProtoBuf.typeAliasKdoc)
    is EnumEntrySyntheticClassDescriptor -> (containingDeclaration as? DeserializedClassDescriptor)
        ?.findEnumEntryProto(name)
        ?.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryKdoc)
    is PropertyAccessorDescriptor -> (correspondingProperty as? DeserializedPropertyDescriptor)?.proto?.getExtensionOrNull(
        if (this is PropertyGetterDescriptor) KlibMetadataProtoBuf.propertyGetterKdoc else KlibMetadataProtoBuf.propertySetterKdoc
    )
    else -> null
}
