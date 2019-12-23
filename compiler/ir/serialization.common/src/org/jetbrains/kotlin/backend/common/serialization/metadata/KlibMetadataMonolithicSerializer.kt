package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.DescriptorSerializer

// TODO: need a refactoring between IncrementalSerializer and MonolithicSerializer.
class KlibMetadataMonolithicSerializer(
    languageVersionSettings: LanguageVersionSettings,
    metadataVersion: BinaryVersion,
    descriptorTable: DescriptorTable,
    skipExpects: Boolean,
    includeOnlyModuleContent: Boolean = false
) : KlibMetadataSerializer(languageVersionSettings, metadataVersion, descriptorTable, skipExpects, includeOnlyModuleContent) {

    private fun serializePackageFragment(fqName: FqName, module: ModuleDescriptor): List<ProtoBuf.PackageFragment> {

        val fragments = if (includeOnlyModuleContent) {
            module.packageFragmentProviderForModuleContentWithoutDependencies.getPackageFragments(fqName)
        } else {
            module.getPackage(fqName).fragments.filter { it.module == module }
        }

        if (fragments.isEmpty()) return emptyList()

        val classifierDescriptors = DescriptorSerializer.sort(
            fragments.flatMap {
                it.getMemberScope().getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)
            }
        )

        val topLevelDescriptors = DescriptorSerializer.sort(
            fragments.flatMap { fragment ->
                fragment.getMemberScope().getDescriptorsFiltered(DescriptorKindFilter.CALLABLES)
            }
        )

        return serializeDescriptors(fqName, classifierDescriptors, topLevelDescriptors)
    }

    fun serializeModule(moduleDescriptor: ModuleDescriptor): SerializedMetadata {

        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()
        val emptyPackages = mutableListOf<String>()

        for (packageFqName in getPackagesFqNames(moduleDescriptor)) {
            val packageProtos =
                serializePackageFragment(packageFqName, moduleDescriptor)
            if (packageProtos.isEmpty()) continue

            val packageFqNameStr = packageFqName.asString()

            if (packageProtos.all { it.getExtension(KlibMetadataProtoBuf.isEmpty)}) {
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
