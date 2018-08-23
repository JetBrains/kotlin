package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.createKonanModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.interop.InteropFqNames
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.exportForwardDeclarations
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.konan.library.packageFqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

// FIXME: ddol: this is a temporary solution, to be refactored into some global resolution context
interface KonanModuleDescriptorFactory {

    fun createModuleDescriptor(
            libraryReader: KonanLibrary,
            specifics: LanguageVersionSettings,
            storageManager: StorageManager = LockBasedStorageManager()
    ): ModuleDescriptor
}

object DefaultKonanModuleDescriptorFactory: KonanModuleDescriptorFactory {

    override fun createModuleDescriptor(
            libraryReader: KonanLibrary,
            specifics: LanguageVersionSettings,
            storageManager: StorageManager
    ): ModuleDescriptorImpl {

        val libraryProto = parseModuleHeader(libraryReader.moduleHeaderData)

        val moduleName = libraryProto.moduleName

        val moduleDescriptor = createKonanModuleDescriptor(
                Name.special(moduleName),
                storageManager,
                origin = DeserializedKonanModuleOrigin(libraryReader)
        )
        val deserializationConfiguration = CompilerDeserializationConfiguration(specifics)

        val provider = createPackageFragmentProvider(
                libraryReader,
                libraryProto.packageFragmentNameList,
                storageManager,
                moduleDescriptor,
                deserializationConfiguration)

        moduleDescriptor.initialize(provider)

        return moduleDescriptor
    }

    private fun createPackageFragmentProvider(
            libraryReader: KonanLibrary,
            fragmentNames: List<String>,
            storageManager: StorageManager,
            moduleDescriptor: ModuleDescriptor,
            configuration: DeserializationConfiguration
    ): PackageFragmentProvider {

        val deserializedPackageFragments = fragmentNames.map{
            KonanPackageFragment(it, libraryReader, storageManager, moduleDescriptor)
        }

        val syntheticPackageFragments = getSyntheticPackageFragments(
                libraryReader,
                moduleDescriptor,
                deserializedPackageFragments)

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments + syntheticPackageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
                moduleDescriptor,
                notFoundClasses,
                KonanSerializerProtocol)

        val components = DeserializationComponents(
                storageManager,
                moduleDescriptor,
                configuration,
                DeserializedClassDataFinder(provider),
                annotationAndConstantLoader,
                provider,
                LocalClassifierTypeSettings.Default,
                ErrorReporter.DO_NOTHING,
                LookupTracker.DO_NOTHING,
                NullFlexibleTypeDeserializer,
                emptyList(),
                notFoundClasses,
                ContractDeserializer.DEFAULT,
                extensionRegistryLite = KonanSerializerProtocol.extensionRegistry)

        for (packageFragment in deserializedPackageFragments) {
            packageFragment.initialize(components)
        }

        return provider
    }

    private fun getSyntheticPackageFragments(
            libraryReader: KonanLibrary,
            moduleDescriptor: ModuleDescriptor,
            konanPackageFragments: List<KonanPackageFragment>
    ): List<PackageFragmentDescriptor> {

        if (!libraryReader.isInterop) return emptyList()

        val packageFqName = libraryReader.packageFqName
                ?: error("Inconsistent manifest: interop library ${libraryReader.libraryName} should have `package` specified")

        val exportForwardDeclarations = libraryReader.exportForwardDeclarations

        val interopPackageFragments = konanPackageFragments.filter { it.fqName == packageFqName }

        val result = mutableListOf<PackageFragmentDescriptor>()

        // Allow references to forwarding declarations to be resolved into classifiers declared in this library:
        listOf(InteropFqNames.cNamesStructs, InteropFqNames.objCNamesClasses, InteropFqNames.objCNamesProtocols).mapTo(result) { fqName ->
            ClassifierAliasingPackageFragmentDescriptor(interopPackageFragments, moduleDescriptor, fqName)
        }
        // TODO: use separate namespaces for structs, enums, Objective-C protocols etc.

        result.add(ExportedForwardDeclarationsPackageFragmentDescriptor(moduleDescriptor, packageFqName, exportForwardDeclarations))

        return result
    }
}
