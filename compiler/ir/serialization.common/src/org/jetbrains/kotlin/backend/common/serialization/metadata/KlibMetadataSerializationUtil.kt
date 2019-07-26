/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.backend.common.serialization.isSerializableExpectClass
import org.jetbrains.kotlin.backend.common.serialization.newDescriptorUniqId
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CALLABLES
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CLASSIFIERS
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

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

class KlibMetadataSerializationUtil(val languageVersionSettings: LanguageVersionSettings, val metadataVersion: BinaryVersion, val declarationTable: DeclarationTable) {

    lateinit var serializerContext: SerializerContext

    val sourceFileMap = SourceFileMap()

    data class SerializerContext(
        val serializerExtension: KlibMetadataSerializerExtension,
        val topSerializer: DescriptorSerializer,
        var classSerializer: DescriptorSerializer = topSerializer
    )

    fun declarationTableHandler(declarationDescriptor: DeclarationDescriptor): KlibMetadataProtoBuf.DescriptorUniqId? {
        val index = declarationTable.descriptorTable.get(declarationDescriptor)
        return index?.let { newDescriptorUniqId(it) }
    }

    private fun createNewContext(): SerializerContext {

        val extension = KlibMetadataSerializerExtension(
            languageVersionSettings,
            metadataVersion,
            ::declarationTableHandler,
            { descriptor ->  (descriptor as? DeclarationDescriptorWithSource)?.let {
                sourceFileMap.assign(it.source.containingFile)
            }
            },
            KlibMetadataStringTable()
        )
        return SerializerContext(
            extension,
            DescriptorSerializer.createTopLevel(extension)
        )
    }

    private inline fun <T> withNewContext(crossinline block: SerializerContext.() -> T): T {
        serializerContext = createNewContext()
        return with(serializerContext, block)
    }

    private fun serializeClass(packageName: FqName,
                               builder: KlibMetadataProtoBuf.LinkDataClasses.Builder,
                               classDescriptor: ClassDescriptor) {
        with(serializerContext) {
            val previousSerializer = classSerializer

            classSerializer = DescriptorSerializer.create(classDescriptor, serializerExtension, classSerializer)

            val classProto = classSerializer.classProto(classDescriptor).build()
                ?: error("Class not serialized: $classDescriptor")

            builder.addClasses(classProto)
            val index = classSerializer.stringTable.getFqNameIndex(classDescriptor)
            builder.addClassName(index)

            serializeClasses(packageName, builder,
                classDescriptor.unsubstitutedInnerClassesScope
                    .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))

            classSerializer = previousSerializer
        }
    }

    private fun serializeClasses(packageName: FqName,
                                 builder: KlibMetadataProtoBuf.LinkDataClasses.Builder,
                                 descriptors: Collection<DeclarationDescriptor>) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor)
            }
        }
    }

    private fun serializePackage(fqName: FqName, module: ModuleDescriptor):
            List<KlibMetadataProtoBuf.LinkDataPackageFragment> {

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

        val result = mutableListOf<KlibMetadataProtoBuf.LinkDataPackageFragment>()

        result += classifierDescriptors.chunked(TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE) { descriptors ->

            withNewContext {

                val classesProto = buildClassesProto { classesBuilder ->
                    serializeClasses(fqName, classesBuilder, descriptors)
                }

                val typeAliases = descriptors.filterIsInstance<TypeAliasDescriptor>()
                val packageProto =
                    if (typeAliases.isNotEmpty()) buildPackageProto(fqName, typeAliases, fragments)
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
                    buildPackageProto(fqName, descriptors, fragments),
                    buildClassesProto {},
                    fqName,
                    descriptors.isEmpty())
            }
        }

        if (result.isEmpty()) {
            result += withNewContext {
                buildFragment(
                    emptyPackageProto(),
                    buildClassesProto {},
                    fqName,
                    true)
            }
        }

        return result
    }


    private fun buildClassesProto(action: (KlibMetadataProtoBuf.LinkDataClasses.Builder) -> Unit): KlibMetadataProtoBuf.LinkDataClasses {
        val classesBuilder = KlibMetadataProtoBuf.LinkDataClasses.newBuilder()
        action(classesBuilder)
        return classesBuilder.build()
    }

    private fun emptyPackageProto(): ProtoBuf.Package = ProtoBuf.Package.newBuilder().build()

    private fun SerializerContext.buildPackageProto(
        fqName: FqName,
        descriptors: List<DeclarationDescriptor>,
        fragments: List<PackageFragmentDescriptor>) = topSerializer.packagePartProto(fqName, descriptors).build()
        ?: error("Package fragments not serialized: $fragments")

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    private fun SerializerContext.buildFragment(
        packageProto: ProtoBuf.Package,
        classesProto: KlibMetadataProtoBuf.LinkDataClasses,
        fqName: FqName,
        isEmpty: Boolean
    ): KlibMetadataProtoBuf.LinkDataPackageFragment {

        val (stringTableProto, nameTableProto) = serializerExtension.stringTable.buildProto()

        return KlibMetadataProtoBuf.LinkDataPackageFragment.newBuilder()
            .setPackage(packageProto)
            .setClasses(classesProto)
            .setIsEmpty(isEmpty)
            .setFqName(fqName.asString())
            .setStringTable(stringTableProto)
            .setNameTable(nameTableProto)
            .build()
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


/*


object JsKlibMetadataSerializationUtil {
    fun serializeMetadata(
        bindingContext: BindingContext,
        jsDescriptor: JsKlibMetadataModuleDescriptor<ModuleDescriptor>,
        languageVersionSettings: LanguageVersionSettings,
        metadataVersion: JsKlibMetadataVersion,
        declarationTableHandler: (DeclarationDescriptor) -> KlibMetadataProtoBuf.DescriptorUniqId?
    ): SerializedMetadata {
        val libraryProto = KlibMetadataProtoBuf.Library.newBuilder()
        jsDescriptor.imported.forEach { libraryProto.addImportedModule(it) }

        val serializedFragments = HashMap<FqName, ProtoBuf.PackageFragment>()
        val module = jsDescriptor.data
        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()

        for (fqName in getPackagesFqNames(
            module
        ).sortedBy { it.asString() }) {
            val fragment =
                serializeDescriptors(
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
        declarationTableHandler: ((DeclarationDescriptor) -> KlibMetadataProtoBuf.DescriptorUniqId?)
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

        val extension = KlibMetadataSerializerExtension(
            languageVersionSettings,
            metadataVersion,
            declarationTableHandler,
            { fileRegistry.getFileId(it) },
            KlibMetadataStringTable()
        )

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
            KlibMetadataProtoBuf.packageFragmentFiles,
            serializeFiles(
                fileRegistry,
                bindingContext,
                AnnotationSerializer(stringTable)
            )
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
    ): KlibMetadataProtoBuf.Files {
        val filesProto = KlibMetadataProtoBuf.Files.newBuilder()
        for ((file, id) in fileRegistry.fileIds.entries.sortedBy { it.value }) {
            val fileProto = KlibMetadataProtoBuf.File.newBuilder()
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
            getSubPackagesFqNames(
                module.getPackage(FqName.ROOT),
                this
            )
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
                getSubPackagesFqNames(
                    descriptor,
                    result
                )
            }
        }
    }

    @JvmStatic
    fun readModuleAsProto(metadata: ByteArray): JsKlibMetadataParts {
        val header = KlibMetadataProtoBuf.Header.parseFrom(metadata, KlibMetadataSerializerProtocol.extensionRegistry)
        val content = KlibMetadataProtoBuf.Library.parseFrom(metadata, KlibMetadataSerializerProtocol.extensionRegistry)
        return JsKlibMetadataParts(
            header,
            content.packageFragmentList,
            content.importedModuleList
        )
    }
}
*/
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
