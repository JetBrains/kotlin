/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.backend.common.serialization.isSerializableExpectClass
import org.jetbrains.kotlin.backend.common.serialization.metadata.JsKlibMetadataParts
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

// TODO: need a refactoring between IncrementalSerializer and MonolithicSerializer.
class KlibMetadataIncrementalSerializer(
    languageVersionSettings: LanguageVersionSettings,
    metadataVersion: BinaryVersion,
    descriptorTable: DescriptorTable
) : KlibMetadataSerializer(languageVersionSettings, metadataVersion, descriptorTable) {

    fun serializeDescriptors(
        bindingContext: BindingContext,
        module: ModuleDescriptor,
        scope: Collection<DeclarationDescriptor>,
        fqName: FqName
    ): ProtoBuf.PackageFragment {

        val skip = fun(descriptor: DeclarationDescriptor): Boolean {
            // TODO: ModuleDescriptor should be able to return the package only with the contents of that module, without dependencies
            if (descriptor.module != module) return true

            if (descriptor is MemberDescriptor && descriptor.isExpect) {
                return !(descriptor is ClassDescriptor && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(
                    descriptor
                ))
            }

            return false
        }

        val classifierDescriptors = scope
            .filterIsInstance<ClassDescriptor>()
            .filter { !it.isExpectMember || it.isSerializableExpectClass }
            .sortedBy { it.fqNameSafe.asString() }

        val topLevelDescriptors = DescriptorSerializer.sort(
            scope
                .filterIsInstance<CallableDescriptor>()
                .filter { !it.isExpectMember }
        )

        // TODO: For now, in the incremental serializer, we assume
        // there is only a single package fragment per file.
        // This is no always the case, actually.
        // But marrying split package fragments with incremental compilation is an endeavour.
        // See monolithic serializer for details.
        return serializeDescriptors(fqName, classifierDescriptors, topLevelDescriptors, bindingContext).single()
    }

    fun serializedMetadata(
        module: ModuleDescriptor,
        fragments: Map<String, List<ByteArray>>
    ): SerializedMetadata {
        val fragmentNames = mutableListOf<String>()
        val fragmentParts = mutableListOf<List<ByteArray>>()

        for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
            fragmentNames += fqName
            fragmentParts += fragment
        }

        val stream = ByteArrayOutputStream()

        serializeHeader(module, fragmentNames).writeDelimitedTo(stream)
        asLibrary().writeTo(stream)
        stream.appendPackageFragments(fragments)

        val moduleLibrary = stream.toByteArray()

        return SerializedMetadata(moduleLibrary, fragmentParts, fragmentNames)
    }

    private fun asLibrary(): KlibMetadataProtoBuf.Library {
        return KlibMetadataProtoBuf.Library.newBuilder().build()
    }

    private fun OutputStream.writeProto(fieldNumber: Int, content: ByteArray) {
        // Message header
        write((fieldNumber shl 3) or 2)
        // Size varint
        var size = content.size
        while (size > 0x7F) {
            write(0x80 or (size and 0x7F))
            size = size ushr 7
        }
        write(size)
        // Fragment itself
        write(content)
    }

    private fun OutputStream.appendPackageFragments(serializedFragments: Map<String, List<ByteArray>>) {
        for ((_, fragments) in serializedFragments.entries.sortedBy { it.key }) {
            for (fragment in fragments) {
                writeProto(KlibMetadataProtoBuf.Library.PACKAGE_FRAGMENT_FIELD_NUMBER, fragment)
            }
        }
    }

    fun readModuleAsProto(metadata: ByteArray, importedModuleList: List<String>): JsKlibMetadataParts {
        val header =
        val inputStream = ByteArrayInputStream(metadata)
        val header = KlibMetadataProtoBuf.Header.parseDelimitedFrom(inputStream, KlibMetadataSerializerProtocol.extensionRegistry)
        val content = KlibMetadataProtoBuf.Library.parseFrom(inputStream, KlibMetadataSerializerProtocol.extensionRegistry)
        return JsKlibMetadataParts(header, content.packageFragmentList, importedModuleList)
    }

    // TODO: For now, in the incremental serializer, we assume
    // there is only a single package fragment per file.
    // This is no always the case, actually.
    // But marrying split package fragments with incremental compilation is an endeavour.
    // See monolithic serializer for details.
    override val TOP_LEVEL_DECLARATION_COUNT_PER_FILE = null
    override val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE = null
}