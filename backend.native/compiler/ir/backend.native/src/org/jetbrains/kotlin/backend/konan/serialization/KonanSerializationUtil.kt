/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer

/*
 * This is Konan specific part of public descriptor 
 * tree serialization and deserialization.
 *
 * It takes care of module and package fragment serializations.
 * The lower level (classes and members) serializations are delegated 
 * to the KonanDescriptorSerializer class.
 * The lower level deserializations are performed by the frontend
 * with MemberDeserializer class.
 */

internal class KonanSerializationUtil(val context: Context, metadataVersion: BinaryVersion) {

    val serializerExtension = KonanSerializerExtension(context, metadataVersion)
    val topSerializer = KonanDescriptorSerializer.createTopLevel(context, serializerExtension)
    var classSerializer: KonanDescriptorSerializer = topSerializer

    fun serializeClass(packageName: FqName,
        builder: KonanProtoBuf.LinkDataClasses.Builder,
        classDescriptor: ClassDescriptor) {

        val previousSerializer = classSerializer

        // TODO: this is to filter out object{}. Change me.
        if (classDescriptor.isExported()) 
            classSerializer = KonanDescriptorSerializer.create(context, classDescriptor, serializerExtension)

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

    fun serializeClasses(packageName: FqName, 
        builder: KonanProtoBuf.LinkDataClasses.Builder,
        descriptors: Collection<DeclarationDescriptor>) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor)
            }
        }
    }

    fun serializePackage(fqName: FqName, module: ModuleDescriptor) :
            KonanProtoBuf.LinkDataPackageFragment? {

        // TODO: ModuleDescriptor should be able to return
        // the package only with the contents of that module, without dependencies

        val fragments = module.getPackage(fqName).fragments.filter { it.module == module }
        if (fragments.isEmpty()) return null

        val classifierDescriptors = KonanDescriptorSerializer.sort(
                fragments.flatMap {
                    it.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS)
                }.filter { !it.isExpectMember }
        )

        val members = fragments.flatMap { fragment ->
            DescriptorUtils.getAllDescriptors(fragment.getMemberScope())
        }.filter { !it.isExpectMember }

        val classesBuilder = KonanProtoBuf.LinkDataClasses.newBuilder()

        serializeClasses(fqName, classesBuilder, classifierDescriptors)
        val classesProto = classesBuilder.build()

        val packageProto = topSerializer.packagePartProto(fqName, members).build()
            ?: error("Package fragments not serialized: $fragments")

        val strings = serializerExtension.stringTable
        val (stringTableProto, nameTableProto) = strings.buildProto()

        val isEmpty = members.isEmpty() && classifierDescriptors.isEmpty()
        val fragmentBuilder = KonanProtoBuf.LinkDataPackageFragment.newBuilder()

        val fragmentProto = fragmentBuilder
            .setPackage(packageProto)
            .setFqName(fqName.asString())
            .setClasses(classesProto)
            .setStringTable(stringTableProto)
            .setNameTable(nameTableProto)
            .setIsEmpty(isEmpty)
            .build()

        return fragmentProto
    }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    internal fun serializeModule(moduleDescriptor: ModuleDescriptor): LinkData {
        val libraryProto = KonanProtoBuf.LinkDataLibrary.newBuilder()
        libraryProto.moduleName = moduleDescriptor.name.asString()
        val fragments = mutableListOf<ByteArray>()
        val fragmentNames = mutableListOf<String>()

        getPackagesFqNames(moduleDescriptor).forEach iteration@ {
            val packageProto = serializePackage(it, moduleDescriptor)
            if (packageProto == null) return@iteration

            libraryProto.addPackageFragmentName(it.asString())
            if (packageProto.isEmpty) {
                libraryProto.addEmptyPackage(it.asString())
            }
            fragments.add(packageProto.toByteArray())
            fragmentNames.add(it.asString())
        }
        val libraryAsByteArray = libraryProto.build().toByteArray()
        return LinkData(libraryAsByteArray, fragments, fragmentNames)
    }
}

