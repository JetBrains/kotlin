/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.backend.konan.descriptors.isSerializableExpectClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CALLABLES
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CLASSIFIERS
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.konan.SourceFileMap

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

internal class KonanSerializationUtil(val context: Context, val metadataVersion: BinaryVersion, val declarationTable: DeclarationTable) {

    lateinit var serializerContext: SerializerContext

    val sourceFileMap = SourceFileMap()

    data class SerializerContext(
            val serializerExtension: KonanSerializerExtension,
            val topSerializer: DescriptorSerializer,
            var classSerializer: DescriptorSerializer = topSerializer
    )
    private fun createNewContext(): SerializerContext {
        val extension = KonanSerializerExtension(context, metadataVersion, sourceFileMap, declarationTable)
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
                       builder: KonanProtoBuf.LinkDataClasses.Builder,
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
                         builder: KonanProtoBuf.LinkDataClasses.Builder,
                         descriptors: Collection<DeclarationDescriptor>) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor)
            }
        }
    }

    private fun serializePackage(fqName: FqName, module: ModuleDescriptor):
            List<KonanProtoBuf.LinkDataPackageFragment> {

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

        val result = mutableListOf<KonanProtoBuf.LinkDataPackageFragment>()

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


    private fun buildClassesProto(action: (KonanProtoBuf.LinkDataClasses.Builder) -> Unit): KonanProtoBuf.LinkDataClasses {
        val classesBuilder = KonanProtoBuf.LinkDataClasses.newBuilder()
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
            classesProto: KonanProtoBuf.LinkDataClasses,
            fqName: FqName,
            isEmpty: Boolean
    ): KonanProtoBuf.LinkDataPackageFragment {

        val (stringTableProto, nameTableProto) = serializerExtension.stringTable.buildProto()

        return KonanProtoBuf.LinkDataPackageFragment.newBuilder()
                .setPackage(packageProto)
                .setClasses(classesProto)
                .setIsEmpty(isEmpty)
                .setFqName(fqName.asString())
                .setStringTable(stringTableProto)
                .setNameTable(nameTableProto)
                .build()
    }

    internal fun serializeModule(moduleDescriptor: ModuleDescriptor): SerializedMetadata {
        val libraryProto = KonanProtoBuf.LinkDataLibrary.newBuilder()
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
