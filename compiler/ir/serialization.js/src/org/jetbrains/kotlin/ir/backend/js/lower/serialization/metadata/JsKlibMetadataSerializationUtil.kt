/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.filterOutSourceAnnotations
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

object JsKlibMetadataSerializationUtil {
    fun serializeMetadata(
        bindingContext: BindingContext,
        jsDescriptor: JsKlibMetadataModuleDescriptor<ModuleDescriptor>,
        languageVersionSettings: LanguageVersionSettings,
        metadataVersion: JsKlibMetadataVersion,
        declarationTableHandler: (DeclarationDescriptor) -> JsKlibMetadataProtoBuf.DescriptorUniqId?
    ): SerializedMetadata {
        val libraryProto = JsKlibMetadataProtoBuf.Library.newBuilder()
        jsDescriptor.imported.forEach { libraryProto.addImportedModule(it) }

        val serializedFragments = HashMap<FqName, ProtoBuf.PackageFragment>()
        val module = jsDescriptor.data
        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()

        for (fqName in getPackagesFqNames(module).sortedBy { it.asString() }) {
            val fragment = serializeDescriptors(
                bindingContext, module,
                module.getPackage(fqName).memberScope.getContributedDescriptors(),
                fqName, languageVersionSettings, metadataVersion, declarationTableHandler
            )

            if (!fragment.isEmpty()) {
                serializedFragments[fqName] = fragment
            }
        }

        for ((fqName, fragment) in serializedFragments.entries.sortedBy { (fqName, _) -> fqName.asString() }) {
            libraryProto.addPackageFragment(fragment)
            fragments.add(listOf(fragment.toByteArray()))
            fragmentNames.add(fqName.asString())
        }

        val libraryAsByteArray = libraryProto.build().toByteArray()
        return SerializedMetadata(libraryAsByteArray, fragments, fragmentNames)
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

    private fun ProtoBuf.PackageFragment.isEmpty(): Boolean =
        class_Count == 0 && `package`.let { it.functionCount == 0 && it.propertyCount == 0 && it.typeAliasCount == 0 }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        return mutableSetOf<FqName>().apply {
            getSubPackagesFqNames(module.getPackage(FqName.ROOT), this)
            add(FqName.ROOT)
        }
    }

    private fun getSubPackagesFqNames(packageView: PackageViewDescriptor, result: MutableSet<FqName>) {
        val fqName = packageView.fqName
        if (!fqName.isRoot) {
            result.add(fqName)
        }

        for (descriptor in packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)) {
            if (descriptor is PackageViewDescriptor) {
                getSubPackagesFqNames(descriptor, result)
            }
        }
    }

    @JvmStatic
    fun readModuleAsProto(metadata: ByteArray): JsKlibMetadataParts {
        val header = JsKlibMetadataProtoBuf.Header.parseFrom(metadata, JsKlibMetadataSerializerProtocol.extensionRegistry)
        val content = JsKlibMetadataProtoBuf.Library.parseFrom(metadata, JsKlibMetadataSerializerProtocol.extensionRegistry)
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
