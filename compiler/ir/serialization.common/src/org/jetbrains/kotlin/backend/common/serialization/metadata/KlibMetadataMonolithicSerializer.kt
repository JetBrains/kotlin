/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.backend.common.serialization.isSerializableExpectClass
import org.jetbrains.kotlin.backend.common.serialization.newDescriptorUniqId
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.KlibMetadataFileRegistry
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
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.io.ByteArrayOutputStream

internal fun <T, R> Iterable<T>.maybeChunked(size: Int?, transform: (List<T>) -> R): List<R>
    = size?.let { this.chunked(size, transform) } ?: listOf(transform(this.toList()))

abstract class KlibMetadataSerializer(
    val languageVersionSettings: LanguageVersionSettings,
    val metadataVersion: BinaryVersion,
    val descriptorTable: DescriptorTable
) {

    val fileRegistry = KlibMetadataFileRegistry()

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
            {descriptor -> fileRegistry.getFileId(descriptor) } ,
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
        isEmpty: Boolean,
        bindingContext: BindingContext
    ): ProtoBuf.PackageFragment {

        val (stringTableProto, nameTableProto) = serializerExtension.stringTable.buildProto()

        // TODO: we place files table to each and every fragment.
        // Need to refactor it out sonehow.
        val files = serializeFiles(fileRegistry, bindingContext, AnnotationSerializer(serializerExtension.stringTable))

        return ProtoBuf.PackageFragment.newBuilder()
            .setPackage(packageProto)
            .addAllClass_(classesProto.map { it.first })
            .setStrings(stringTableProto)
            .setQualifiedNames(nameTableProto)
            .also { packageFragment ->
                classesProto.forEach {
                    packageFragment.addExtension(KlibMetadataProtoBuf.className, it.second )
                }
                packageFragment.setExtension(KlibMetadataProtoBuf.packageFragmentFiles, files)
                packageFragment.setExtension(KlibMetadataProtoBuf.isEmpty, isEmpty)
                packageFragment.setExtension(KlibMetadataProtoBuf.fqName, fqName.asString())
            }
            .build()
    }

    private fun serializeClass(packageName: FqName,
                               classDescriptor: ClassDescriptor): List<Pair<ProtoBuf.Class, Int>> {
        with(serializerContext) {
            val previousSerializer = classSerializer

            classSerializer = DescriptorSerializer.create(classDescriptor, serializerExtension, classSerializer)
            val classProto = classSerializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
            //builder.addClass(classProto)

            val index = classSerializer.stringTable.getFqNameIndex(classDescriptor)
            //builder.addExtension(KlibMetadataProtoBuf.className, index)

            val classes = serializeClasses(packageName/*, builder*/,
                classDescriptor.unsubstitutedInnerClassesScope
                    .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))

            classSerializer = previousSerializer
            return classes + Pair(classProto, index)
        }
    }

    protected fun serializeClasses(packageName: FqName,
                                 //builder: ProtoBuf.PackageFragment.Builder,
                                 descriptors: Collection<DeclarationDescriptor>): List<Pair<ProtoBuf.Class, Int>> {

        return descriptors.filterIsInstance<ClassDescriptor>().flatMap {
            serializeClass(packageName, /*builder, */it)
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
        topLevelDescriptors: List<DeclarationDescriptor>,
        bindingContext: BindingContext
    ): List<ProtoBuf.PackageFragment> {

        val result = mutableListOf<ProtoBuf.PackageFragment>()

        result += classifierDescriptors.maybeChunked(TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE) { descriptors ->

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
                    descriptors.isEmpty(),
                    bindingContext
                )
            }
        }

        result += topLevelDescriptors.maybeChunked(TOP_LEVEL_DECLARATION_COUNT_PER_FILE) { descriptors ->
            withNewContext {
                buildFragment(
                    buildPackageProto(fqName, descriptors),
                    //buildClassesProto {},
                    emptyList(),
                    fqName,
                    descriptors.isEmpty(),
                    bindingContext
                )
            }
        }

        if (result.isEmpty()) {
            result += withNewContext {
                buildFragment(
                    emptyPackageProto(),
                    //buildClassesProto {},
                    emptyList(),
                    fqName,
                    true,
                    bindingContext
                )
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
        for ((file, id) in fileRegistry.fileIds.entries.sortedBy { it.value }) {
            val fileProto = KlibMetadataProtoBuf.File.newBuilder()
            if (id != filesProto.fileCount) {
                fileProto.id = id
            }
            val annotations = when (file) {
                is KotlinPsiFileMetadata -> file.ktFile.annotationEntries.map { bindingContext[BindingContext.ANNOTATION, it]!! }
                //is KotlinDeserializedFileMetadata -> file.packageFragment.fileMap[file.fileId]!!.annotations
                else -> TODO("support other file types")
            }
            for (annotation in annotations.filterOutSourceAnnotations()) {
                fileProto.addAnnotation(serializer.serializeAnnotation(annotation))
            }
            val name = when (file) {
                is KotlinPsiFileMetadata -> file.ktFile.getName()
                else -> TODO("support other file types")
            }
            fileProto.name = name
            filesProto.addFile(fileProto)
        }
        return filesProto.build()
    }

    protected fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    fun serializeHeader(
        moduleDescriptor: ModuleDescriptor,
        fragmentNames: List<String>,
        emptyPackages: List<String> = emptyList()
    ): KlibMetadataProtoBuf.Header {
        val header = KlibMetadataProtoBuf.Header.newBuilder()

        header.moduleName = moduleDescriptor.name.asString()

        if (languageVersionSettings.isPreRelease()) {
            header.flags = 1
        }

        val experimentalAnnotationFqNames = languageVersionSettings.getFlag(AnalysisFlags.experimental)
        if (experimentalAnnotationFqNames.isNotEmpty()) {
            val stringTable = StringTableImpl()
            for (fqName in experimentalAnnotationFqNames) {
                val descriptor = moduleDescriptor.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_ALREADY_TRACKED) ?: continue
                header.addAnnotation(ProtoBuf.Annotation.newBuilder().apply {
                    id = stringTable.getFqNameIndex(descriptor)
                })
            }
            val (strings, qualifiedNames) = stringTable.buildProto()
            header.strings = strings
            header.qualifiedNames = qualifiedNames
        }
        fragmentNames.forEach {
            header.addPackageFragmentName(it)
        }
        emptyPackages.forEach {
            header.addEmptyPackage(it)
        }
        return header.build()
    }

    // For platform libraries we get HUGE files.
    // Indexing them in IDEA takes ages.
    // So we split them into chunks.
    abstract protected val TOP_LEVEL_DECLARATION_COUNT_PER_FILE: Int?
    abstract protected val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE: Int?
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
    descriptorTable: DescriptorTable,
    val bindingContext: BindingContext
) : KlibMetadataSerializer(languageVersionSettings, metadataVersion, descriptorTable) {

    protected fun serializePackageFragment(fqName: FqName, module: ModuleDescriptor, bindingContext: BindingContext):
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

        return serializeDescriptors(fqName, classifierDescriptors, topLevelDescriptors, bindingContext)
    }

    fun serializeModule(moduleDescriptor: ModuleDescriptor): SerializedMetadata {

        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()
        val emptyPackages = mutableListOf<String>()

        getPackagesFqNames(moduleDescriptor).forEach iteration@{ packageFqName ->
            val packageProtos =
                serializePackageFragment(packageFqName, moduleDescriptor, bindingContext)
            if (packageProtos.isEmpty()) return@iteration

            val packageFqNameStr = packageFqName.asString()

            //header.addPackageFragmentName(packageFqNameStr)
            if (packageProtos.all { it.getExtension(KlibMetadataProtoBuf.isEmpty)}) {
                //header.addEmptyPackage(packageFqNameStr)
                emptyPackages.add(packageFqNameStr)
            }
            fragments.add(packageProtos.map { it.toByteArray() })
            fragmentNames.add(packageFqNameStr)

        }
        val header = serializeHeader(moduleDescriptor, fragmentNames, emptyPackages)

        val libraryAsByteArray = header.toByteArray()
        return SerializedMetadata(libraryAsByteArray, fragments, fragmentNames)
    }

    // For platform libraries we get HUGE files.
    // Indexing them in IDEA takes ages.
    // So we split them into chunks.
    override val TOP_LEVEL_DECLARATION_COUNT_PER_FILE = 128
    override val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE = 64

}


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
