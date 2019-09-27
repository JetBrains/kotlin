package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.backend.common.serialization.isSerializableExpectClass
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.DescriptorSerializer


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

    protected fun serializePackageFragment(fqName: FqName,
                                           module: ModuleDescriptor,
                                           bindingContext: BindingContext
    ):
            List<ProtoBuf.PackageFragment> {

        // TODO: ModuleDescriptor should be able to return
        // the package only with the contents of that module, without dependencies

        val fragments = module.getPackage(fqName).fragments.filter { it.module == module }
        if (fragments.isEmpty()) return emptyList()

        val classifierDescriptors = DescriptorSerializer.sort(
            fragments.flatMap {
                it.getMemberScope().getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)
            }.filter { !it.isExpectMember || it.isSerializableExpectClass }
        )

        val topLevelDescriptors = DescriptorSerializer.sort(
            fragments.flatMap { fragment ->
                fragment.getMemberScope().getDescriptorsFiltered(DescriptorKindFilter.CALLABLES)
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
