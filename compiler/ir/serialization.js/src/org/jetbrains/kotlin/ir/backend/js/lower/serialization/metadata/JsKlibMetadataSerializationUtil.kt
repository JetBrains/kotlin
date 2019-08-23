/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.filterOutSourceAnnotations
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

object JsKlibMetadataSerializationUtil {

    fun serializedMetadata(
        module: ModuleDescriptor,
        settings: LanguageVersionSettings,
        importedModules: List<String>,
        fragments: Map<String, List<ByteArray>>
    ): SerializedMetadata {
        val fragmentNames = mutableListOf<String>()
        val fragmentParts = mutableListOf<List<ByteArray>>()

        for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
            fragmentNames += fqName
            fragmentParts += fragment
        }

        val stream = ByteArrayOutputStream()

        serializeHeader(module, null, settings).writeDelimitedTo(stream)
        asLibrary().writeTo(stream)
        stream.appendPackageFragments(fragments)
        importedModules.forEach {
            stream.writeProto(JsKlibMetadataProtoBuf.Library.IMPORTED_MODULE_FIELD_NUMBER, it.toByteArray())
        }

        val moduleLibrary = stream.toByteArray()

        return SerializedMetadata(moduleLibrary, fragmentParts, fragmentNames)
    }

    private fun asLibrary(): JsKlibMetadataProtoBuf.Library {
        return JsKlibMetadataProtoBuf.Library.newBuilder().build()
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
                writeProto(JsKlibMetadataProtoBuf.Library.PACKAGE_FRAGMENT_FIELD_NUMBER, fragment)
            }
        }
    }

    fun serializeHeader(
        module: ModuleDescriptor, packageFqName: FqName?, languageVersionSettings: LanguageVersionSettings
    ): JsKlibMetadataProtoBuf.Header {
        val header = JsKlibMetadataProtoBuf.Header.newBuilder()

        if (packageFqName != null) {
            header.packageFqName = packageFqName.asString()
        }

        if (languageVersionSettings.isPreRelease()) {
            header.flags = 1
        }

        val experimentalAnnotationFqNames = languageVersionSettings.getFlag(AnalysisFlags.experimental)
        if (experimentalAnnotationFqNames.isNotEmpty()) {
            val stringTable = StringTableImpl()
            for (fqName in experimentalAnnotationFqNames) {
                val descriptor = module.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_ALREADY_TRACKED) ?: continue
                header.addAnnotation(ProtoBuf.Annotation.newBuilder().apply {
                    id = stringTable.getFqNameIndex(descriptor)
                })
            }
            val (strings, qualifiedNames) = stringTable.buildProto()
            header.strings = strings
            header.qualifiedNames = qualifiedNames
        }

        // TODO: write JS code binary version

        return header.build()
    }

    fun serializeDescriptors(
        bindingContext: BindingContext,
        module: ModuleDescriptor,
        scope: Collection<DeclarationDescriptor>,
        fqName: FqName,
        languageVersionSettings: LanguageVersionSettings,
        metadataVersion: BinaryVersion,
        declarationTableHandler: ((DeclarationDescriptor) -> JsKlibMetadataProtoBuf.DescriptorUniqId?)
    ): ProtoBuf.PackageFragment {
        val builder = ProtoBuf.PackageFragment.newBuilder()

        val skip = fun(descriptor: DeclarationDescriptor): Boolean {
            // TODO: ModuleDescriptor should be able to return the package only with the contents of that module, without dependencies
            if (descriptor.module != module) return true

            if (descriptor is MemberDescriptor && descriptor.isExpect) {
                return !(descriptor is ClassDescriptor && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(descriptor))
            }

            return false
        }

        val fileRegistry = JsKlibMetadataFileRegistry()
        val extension = JsKlibMetadataSerializerExtension(fileRegistry, languageVersionSettings, metadataVersion, declarationTableHandler)

        val classDescriptors = scope.filterIsInstance<ClassDescriptor>().sortedBy { it.fqNameSafe.asString() }

        fun serializeClasses(descriptors: Collection<DeclarationDescriptor>, parentSerializer: DescriptorSerializer) {
            for (descriptor in descriptors) {
                if (descriptor !is ClassDescriptor || skip(descriptor)) continue

                val serializer = DescriptorSerializer.create(descriptor, extension, parentSerializer)
                serializeClasses(descriptor.unsubstitutedInnerClassesScope.getContributedDescriptors(), serializer)
                val classProto = serializer.classProto(descriptor).build() ?: error("Class not serialized: $descriptor")
                builder.addClass_(classProto)
            }
        }

        val serializer = DescriptorSerializer.createTopLevel(extension)
        serializeClasses(classDescriptors, serializer)

        val stringTable = extension.stringTable

        val members = scope.filterNot(skip)
        builder.`package` = serializer.packagePartProto(fqName, members).build()

        builder.setExtension(
            JsKlibMetadataProtoBuf.packageFragmentFiles,
            serializeFiles(fileRegistry, bindingContext, AnnotationSerializer(stringTable))
        )

        val (strings, qualifiedNames) = stringTable.buildProto()
        builder.strings = strings
        builder.qualifiedNames = qualifiedNames

        return builder.build()
    }

    private fun serializeFiles(
        fileRegistry: JsKlibMetadataFileRegistry,
        bindingContext: BindingContext,
        serializer: AnnotationSerializer
    ): JsKlibMetadataProtoBuf.Files {
        val filesProto = JsKlibMetadataProtoBuf.Files.newBuilder()
        for ((file, id) in fileRegistry.fileIds.entries.sortedBy { it.value }) {
            val fileProto = JsKlibMetadataProtoBuf.File.newBuilder()
            if (id != filesProto.fileCount) {
                fileProto.id = id
            }
            val annotations = when (file) {
                is KotlinPsiFileMetadata -> file.ktFile.annotationEntries.map { bindingContext[BindingContext.ANNOTATION, it]!! }
                is KotlinDeserializedFileMetadata -> file.packageFragment.fileMap[file.fileId]!!.annotations
            }
            for (annotation in annotations.filterOutSourceAnnotations()) {
                fileProto.addAnnotation(serializer.serializeAnnotation(annotation))
            }
            filesProto.addFile(fileProto)
        }
        return filesProto.build()
    }

    @JvmStatic
    fun readModuleAsProto(metadata: ByteArray): JsKlibMetadataParts {
        val inputStream = ByteArrayInputStream(metadata)
        val header = JsKlibMetadataProtoBuf.Header.parseDelimitedFrom(inputStream, JsKlibMetadataSerializerProtocol.extensionRegistry)
        val content = JsKlibMetadataProtoBuf.Library.parseFrom(inputStream, JsKlibMetadataSerializerProtocol.extensionRegistry)
        return JsKlibMetadataParts(header, content.packageFragmentList, content.importedModuleList)
    }
}

data class JsKlibMetadataParts(
    val header: JsKlibMetadataProtoBuf.Header,
    val body: List<ProtoBuf.PackageFragment>,
    val importedModules: List<String>
)

internal fun DeclarationDescriptor.extractFileId(): Int? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtension(JsKlibMetadataProtoBuf.classContainingFileId)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtension(JsKlibMetadataProtoBuf.functionContainingFileId)
    is DeserializedPropertyDescriptor -> proto.getExtension(JsKlibMetadataProtoBuf.propertyContainingFileId)
    else -> null
}
