/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.createInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.DeserializedKonanModule
import org.jetbrains.kotlin.backend.konan.descriptors.createKonanModuleDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.metadata.KonanLinkData.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

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

/* ------------ Deserializer part ------------------------------------------*/

object NullFlexibleTypeDeserializer : FlexibleTypeDeserializer {
    override fun create(proto: ProtoBuf.Type, flexibleId: String, 
        lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
            error("Illegal use of flexible type deserializer.")
        }
}

fun createKonanPackageFragmentProvider(
        reader: KonanLibraryReader,
        fragmentNames: List<String>,
        storageManager: StorageManager, module: ModuleDescriptor,
        configuration: DeserializationConfiguration): PackageFragmentProvider {

    val packageFragments = fragmentNames.map{ 
        KonanPackageFragment(it, reader, storageManager, module)
    }

    val interopLibrary = createInteropLibrary(reader)

    val syntheticInteropPackageFragments =
            interopLibrary?.createSyntheticPackages(module, packageFragments) ?: emptyList()

    val provider = PackageFragmentProviderImpl(packageFragments + syntheticInteropPackageFragments)

    val notFoundClasses = NotFoundClasses(storageManager, module)

    val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(module, notFoundClasses, KonanSerializerProtocol)

    val components = DeserializationComponents(
        storageManager, module, configuration, 
        DeserializedClassDataFinder(provider),
        annotationAndConstantLoader,
        provider, 
        LocalClassifierTypeSettings.Default, 
        ErrorReporter.DO_NOTHING,
        LookupTracker.DO_NOTHING, NullFlexibleTypeDeserializer,
        emptyList(), notFoundClasses, ContractDeserializer.DEFAULT, extensionRegistryLite = KonanSerializerProtocol.extensionRegistry )

        for (packageFragment in packageFragments) {
            packageFragment.initialize(components)
        }

    return provider
}

public fun parsePackageFragment(packageData: ByteArray): LinkDataPackageFragment =
    LinkDataPackageFragment.parseFrom(packageData,
        KonanSerializerProtocol.extensionRegistry)

public fun parseModuleHeader(libraryData: ByteArray): LinkDataLibrary =
    LinkDataLibrary.parseFrom(libraryData,
        KonanSerializerProtocol.extensionRegistry)

public fun emptyPackages(libraryData: ByteArray) 
    = parseModuleHeader(libraryData).emptyPackageList

internal fun deserializeModule(languageVersionSettings: LanguageVersionSettings,
                               reader: KonanLibraryReader): ModuleDescriptorImpl {

    val libraryProto = parseModuleHeader(reader.moduleHeaderData)

    val moduleName = libraryProto.moduleName

    val storageManager = LockBasedStorageManager()
    val moduleDescriptor = createKonanModuleDescriptor(
            Name.special(moduleName), storageManager,
            origin = DeserializedKonanModule(reader)
    )
    val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    val provider = createKonanPackageFragmentProvider(
            reader,
            libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor, deserializationConfiguration)

    moduleDescriptor.initialize(provider)

    return moduleDescriptor
}


/* ------------ Serializer part ------------------------------------------*/

internal class KonanSerializationUtil(val context: Context) {

    val serializerExtension = KonanSerializerExtension(context)
    val topSerializer = KonanDescriptorSerializer.createTopLevel(serializerExtension)
    var classSerializer: KonanDescriptorSerializer = topSerializer

    fun serializeClass(packageName: FqName,
        builder: KonanLinkData.LinkDataClasses.Builder,
        classDescriptor: ClassDescriptor) {

        val previousSerializer = classSerializer

        // TODO: this is to filter out object{}. Change me.
        if (classDescriptor.isExported()) 
            classSerializer = KonanDescriptorSerializer.create(classDescriptor, serializerExtension)

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
        builder: KonanLinkData.LinkDataClasses.Builder,
        descriptors: Collection<DeclarationDescriptor>) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor)
            }
        }
    }

    fun serializePackage(fqName: FqName, module: ModuleDescriptor) : 
        KonanLinkData.LinkDataPackageFragment? {

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

        val classesBuilder = KonanLinkData.LinkDataClasses.newBuilder()

        serializeClasses(fqName, classesBuilder, classifierDescriptors)
        val classesProto = classesBuilder.build()

        val packageProto = topSerializer.packagePartProto(fqName, members).build()
            ?: error("Package fragments not serialized: $fragments")

        val strings = serializerExtension.stringTable
        val (stringTableProto, nameTableProto) = strings.buildProto()

        val isEmpty = members.isEmpty() && classifierDescriptors.isEmpty()
        val fragmentBuilder = KonanLinkData.LinkDataPackageFragment.newBuilder()

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
        val libraryProto = KonanLinkData.LinkDataLibrary.newBuilder()
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

