/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.backend.common.serialization.isSerializableExpectClass
import org.jetbrains.kotlin.backend.common.serialization.newDescriptorUniqId
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataFileRegistry
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.KotlinDeserializedFileMetadata
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.KotlinPsiFileMetadata
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.filterOutSourceAnnotations
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CALLABLES
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CLASSIFIERS
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.io.ByteArrayOutputStream

fun SourceFileMap.fileIdHandler(descriptor: DeclarationDescriptor): Int? {
    return (descriptor as? DeclarationDescriptorWithSource)?.let {
        this.assign(it.source.containingFile)
    }
}

open class KlibMetadataSerializer(
    val languageVersionSettings: LanguageVersionSettings,
    val metadataVersion: BinaryVersion,
    val descriptorTable: DescriptorTable
) {

    val sourceFileMap = SourceFileMap()

    lateinit var serializerContext: SerializerContext

    data class SerializerContext(
        val serializerExtension: KlibMetadataSerializerExtension,
        val topSerializer: DescriptorSerializer,
        var classSerializer: DescriptorSerializer = topSerializer
    )

    fun declarationTableHandler(declarationDescriptor: DeclarationDescriptor): KlibMetadataProtoBuf.DescriptorUniqId? {
        val index = descriptorTable.get(declarationDescriptor)
        return index?.let { newDescriptorUniqId(it) }
    }

    protected fun createNewContext(): SerializerContext {

        val extension = KlibMetadataSerializerExtension(
            languageVersionSettings,
            metadataVersion,
            ::declarationTableHandler,
            {descriptor -> sourceFileMap.fileIdHandler(descriptor) } ,
            KlibMetadataStringTable()
        )
        return SerializerContext(
            extension,
            DescriptorSerializer.createTopLevel(extension)
        )
    }

    protected inline fun <T> withNewContext(crossinline block: SerializerContext.() -> T): T {
        serializerContext = createNewContext()
        return with(serializerContext, block)
    }

    private fun SerializerContext.buildFragment(
        packageProto: ProtoBuf.Package,
        classesProto: List<Pair<ProtoBuf.Class, Int>>,
        fqName: FqName,
        isEmpty: Boolean
    ): ProtoBuf.PackageFragment {

        val (stringTableProto, nameTableProto) = serializerExtension.stringTable.buildProto()

        return ProtoBuf.PackageFragment.newBuilder()
            .setPackage(packageProto)
            .addAllClass_(classesProto.map { it.first })
            .also { packageFragment ->
                classesProto.forEach {
                    packageFragment.addExtension(KlibMetadataProtoBuf.className, it.second )
                }
            }
            .setIsEmpty(isEmpty)
            .setFqName(fqName.asString())
            .setStringTable(stringTableProto)
            .setNameTable(nameTableProto)
            .build()
    }

    private fun serializeClass(packageName: FqName,
                               classDescriptor: ClassDescriptor): Pair<ProtoBuf.Class, Int> {
        with(serializerContext) {
            val previousSerializer = classSerializer

            classSerializer = DescriptorSerializer.create(classDescriptor, serializerExtension, classSerializer)
            val classProto = classSerializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
            //builder.addClass(classProto)

            val index = classSerializer.stringTable.getFqNameIndex(classDescriptor)
            //builder.addExtension(KlibMetadataProtoBuf.className, index)

            serializeClasses(packageName/*, builder*/,
                classDescriptor.unsubstitutedInnerClassesScope
                    .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))

            classSerializer = previousSerializer
            return Pair(classProto, index)
        }
    }

    protected fun serializeClasses(packageName: FqName,
                                 //builder: ProtoBuf.PackageFragment.Builder,
                                 descriptors: Collection<DeclarationDescriptor>): List<Pair<ProtoBuf.Class, Int>> {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, /*builder, */descriptor)
            }
        }
    }

    private fun emptyPackageProto(): ProtoBuf.Package = ProtoBuf.Package.newBuilder().build()

    private fun SerializerContext.buildPackageProto(
        fqName: FqName,
        descriptors: List<DeclarationDescriptor>) = topSerializer.packagePartProto(fqName, descriptors).build()
        ?: error("Package fragments not serialized: for $descriptors")

    protected fun serializeDescriptors(
        fqName: FqName,
        classifierDescriptors: List<DeclarationDescriptor>,
        topLevelDescriptors: List<DeclarationDescriptor>
    ): List<ProtoBuf.PackageFragment> {

        val result = mutableListOf<ProtoBuf.PackageFragment>()

        result += classifierDescriptors.chunked(TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE) { descriptors ->

            withNewContext {

                //val classesProto = buildClassesProto { classesBuilder ->
                //    serializeClasses(fqName, classesBuilder, descriptors)
                //}
                val classesProto = serializeClasses(fqName, descriptors)

                val typeAliases = descriptors.filterIsInstance<TypeAliasDescriptor>()
                val packageProto =
                    if (typeAliases.isNotEmpty()) buildPackageProto(fqName, typeAliases)
                    else emptyPackageProto()

                buildFragment(
                    packageProto,
                    classesProto,
                    fqName,
                    descriptors.isEmpty()
                )
            }
        }

        result += topLevelDescriptors.chunked(TOP_LEVEL_DECLARATION_COUNT_PER_FILE) { descriptors ->
            withNewContext {
                buildFragment(
                    buildPackageProto(fqName, descriptors),
                    //buildClassesProto {},
                    emptyList(),
                    fqName,
                    descriptors.isEmpty())
            }
        }

        if (result.isEmpty()) {
            result += withNewContext {
                buildFragment(
                    emptyPackageProto(),
                    //buildClassesProto {},
                    emptyList(),
                    fqName,
                    true)
            }
        }

        return result
    }

    private fun serializeFiles(
        fileRegistry: KlibMetadataFileRegistry,
        bindingContext: BindingContext,
        serializer: AnnotationSerializer
    ): KlibMetadataProtoBuf.Files {
        val filesProto = KlibMetadataProtoBuf.Files.newBuilder()

        sourceFileMap.filesAndClear().map { it.name ?: "" }.forEach {
            filesProto.addFile(serializeFile(it))
        }
    }

    private fun serializeFile(file: SourceFile) {

    }

}

/*
 * This is Konan specific part of public descriptor
 * tree serialization and deserialization.
 *
 * It takes care of module and package fragment serializations.
 * The lower level (classes and members) serializations are delegated
 * to the DescriptorSerializer class.
 * The lower level deserializations are performed by the frontend
 * with MemberDeserializer class.
 */

class KlibMetadataMonolithicSerializer(
    languageVersionSettings: LanguageVersionSettings,
    metadataVersion: BinaryVersion,
    descriptorTable: DescriptorTable
) : KlibMetadataSerializer(languageVersionSettings, metadataVersion, descriptorTable) {

    private fun serializePackage(fqName: FqName, module: ModuleDescriptor):
            List<ProtoBuf.PackageFragment> {

        // TODO: ModuleDescriptor should be able to return
        // the package only with the contents of that module, without dependencies

        val fragments = module.getPackage(fqName).fragments.filter { it.module == module }
        if (fragments.isEmpty()) return emptyList()

        val classifierDescriptors = DescriptorSerializer.sort(
            fragments.flatMap {
                it.getMemberScope().getDescriptorsFiltered(CLASSIFIERS)
            }.filter { !it.isExpectMember || it.isSerializableExpectClass }
        )

        val topLevelDescriptors = DescriptorSerializer.sort(
            fragments.flatMap { fragment ->
                fragment.getMemberScope().getDescriptorsFiltered(CALLABLES)
            }.filter { !it.isExpectMember }
        )

        return serializeDescriptors(fqName, classifierDescriptors, topLevelDescriptors)
    }

/*
    private fun buildClassesProto(action: (KlibMetadataProtoBuf.LinkDataClasses.Builder) -> Unit): KlibMetadataProtoBuf.LinkDataClasses {
        val classesBuilder = KlibMetadataProtoBuf.LinkDataClasses.newBuilder()
        action(classesBuilder)
        return classesBuilder.build()
    }
*/
    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    fun serializeModule(moduleDescriptor: ModuleDescriptor): SerializedMetadata {
        val libraryProto = KlibMetadataProtoBuf.LinkDataLibrary.newBuilder()
        libraryProto.moduleName = moduleDescriptor.name.asString()
        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()

        getPackagesFqNames(moduleDescriptor).forEach iteration@{ packageFqName ->
            val packageProtos =
                serializePackage(packageFqName, moduleDescriptor)
            if (packageProtos.isEmpty()) return@iteration

            val packageFqNameStr = packageFqName.asString()
            libraryProto.addPackageFragmentName(packageFqNameStr)
            if (packageProtos.all { it.isEmpty }) {
                libraryProto.addEmptyPackage(packageFqNameStr)
            }
            fragments.add(packageProtos.map { it.toByteArray() })
            fragmentNames.add(packageFqNameStr)

        }

        sourceFileMap.filesAndClear().map { it.name ?: "" }.forEach {
            libraryProto.addFile(it)
        }

        val libraryAsByteArray = libraryProto.build().toByteArray()
        return SerializedMetadata(libraryAsByteArray, fragments, fragmentNames)
    }
}

private const val TOP_LEVEL_DECLARATION_COUNT_PER_FILE = 128
private const val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE = 64


data class JsKlibMetadataParts(
    val header: KlibMetadataProtoBuf.Header,
    val body: List<ProtoBuf.PackageFragment>,
    val importedModules: List<String>
)

fun DeclarationDescriptor.extractFileId(): Int? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtension(KlibMetadataProtoBuf.classFile)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtension(KlibMetadataProtoBuf.functionFile)
    is DeserializedPropertyDescriptor -> proto.getExtension(KlibMetadataProtoBuf.propertyFile)
    else -> null
}
