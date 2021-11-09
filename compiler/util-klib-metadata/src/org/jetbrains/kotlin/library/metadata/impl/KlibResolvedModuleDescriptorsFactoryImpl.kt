package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.konan.KlibResolvedModuleDescriptorsFactory
import org.jetbrains.kotlin.serialization.konan.KotlinResolvedModuleDescriptors
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer

// TODO: eliminate Native specifics.
class KlibResolvedModuleDescriptorsFactoryImpl(
    override val moduleDescriptorFactory: KlibMetadataModuleDescriptorFactory
): KlibResolvedModuleDescriptorsFactory {

    override fun createResolved(
        resolvedLibraries: KotlinLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        languageVersionSettings: LanguageVersionSettings,
        friendModuleFiles: Set<File>,
        includedLibraryFiles: Set<File>,
        additionalDependencyModules: Iterable<ModuleDescriptorImpl>
    ): KotlinResolvedModuleDescriptors {

        val moduleDescriptors = mutableListOf<ModuleDescriptorImpl>()

        @Suppress("NAME_SHADOWING")
        var builtIns = builtIns

        val friendModuleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        val includedLibraryDescriptors = mutableSetOf<ModuleDescriptorImpl>()

        // Build module descriptors.
        resolvedLibraries.forEach { library, packageAccessHandler ->
            profile("Loading ${library.libraryName}") {

                // MutableModuleContext needs ModuleDescriptorImpl, rather than ModuleDescriptor.
                val moduleDescriptor = createDescriptorOptionalBuiltsIns(
                    library, languageVersionSettings, storageManager, builtIns, packageAccessHandler
                )
                builtIns = moduleDescriptor.builtIns
                moduleDescriptors.add(moduleDescriptor)

                if (friendModuleFiles.contains(library.libraryFile))
                    friendModuleDescriptors.add(moduleDescriptor)
                if (includedLibraryFiles.contains(library.libraryFile))
                    includedLibraryDescriptors.add(moduleDescriptor)
            }
        }

        val forwardDeclarationsModule = createForwardDeclarationsModule(builtIns, storageManager)

        // Set inter-dependencies between module descriptors, add forwarding declarations module.
        for (module in moduleDescriptors) {
            val friends = additionalDependencyModules.toMutableSet()
            if (module in includedLibraryDescriptors)
                friends.addAll(friendModuleDescriptors)
            module.setDependencies(
                // Yes, just to all of them.
                moduleDescriptors + additionalDependencyModules + forwardDeclarationsModule,
                friends
            )
        }

        return KotlinResolvedModuleDescriptors(moduleDescriptors, forwardDeclarationsModule, friendModuleDescriptors)
    }

    fun createForwardDeclarationsModule(
        builtIns: KotlinBuiltIns?,
        storageManager: StorageManager
    ): ModuleDescriptorImpl {

        val module = createDescriptorOptionalBuiltsIns(FORWARD_DECLARATIONS_MODULE_NAME, storageManager, builtIns, SyntheticModulesOrigin)

        fun createPackage(fqName: FqName, supertypeName: String, classKind: ClassKind) =
            ForwardDeclarationsPackageFragmentDescriptor(
                storageManager,
                module,
                fqName,
                Name.identifier(supertypeName),
                classKind
            )

        val packageFragmentProvider = PackageFragmentProviderImpl(
            listOf(
                createPackage(ForwardDeclarationsFqNames.cNamesStructs, "COpaque", ClassKind.CLASS),
                createPackage(ForwardDeclarationsFqNames.objCNamesClasses, "ObjCObjectBase", ClassKind.CLASS),
                createPackage(ForwardDeclarationsFqNames.objCNamesProtocols, "ObjCObject", ClassKind.INTERFACE)
            )
        )

        module.initialize(packageFragmentProvider)
        module.setDependencies(module)

        return module
    }

    private fun createDescriptorOptionalBuiltsIns(
            name: Name,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
            moduleOrigin: KlibModuleOrigin
    ) = if (builtIns != null)
        moduleDescriptorFactory.descriptorFactory.createDescriptor(name, storageManager, builtIns, moduleOrigin)
    else
        moduleDescriptorFactory.descriptorFactory.createDescriptorAndNewBuiltIns(name, storageManager, moduleOrigin)

    private fun createDescriptorOptionalBuiltsIns(
            library: KotlinLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
            packageAccessHandler: PackageAccessHandler?
    ) = if (builtIns != null)
        moduleDescriptorFactory.createDescriptor(library, languageVersionSettings, storageManager, builtIns, packageAccessHandler)
    else
        moduleDescriptorFactory.createDescriptorAndNewBuiltIns(library, languageVersionSettings, storageManager, packageAccessHandler)

    companion object {
        val FORWARD_DECLARATIONS_MODULE_NAME = Name.special("<forward declarations>")
    }
}

/**
 * Package fragment which creates descriptors for forward declarations on demand.
 */
class ForwardDeclarationsPackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,
    fqName: FqName,
    supertypeName: Name,
    classKind: ClassKind
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val declarations = storageManager.createMemoizedFunction(this::createDeclaration)

        private val supertype by storageManager.createLazyValue {
            val descriptor = builtIns.builtInsModule.getPackage(ForwardDeclarationsFqNames.cInterop)
                .memberScope
                .getContributedClassifier(supertypeName, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

            descriptor.defaultType
        }

        private fun createDeclaration(name: Name): ClassDescriptor {
            return ClassDescriptorImpl(
                this@ForwardDeclarationsPackageFragmentDescriptor,
                name,
                Modality.FINAL,
                classKind,
                listOf(supertype),
                SourceElement.NO_SOURCE,
                false,
                LockBasedStorageManager.NO_LOCKS
            ).apply {
                this.initialize(MemberScope.Empty, emptySet(), null)
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation) = declarations(name)

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, "{}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}

// TODO decouple and move interop-specific logic back to Kotlin/Native.
object ForwardDeclarationsFqNames {

    internal val cInterop = FqName("kotlinx.cinterop")

    private val cNames = FqName("cnames")
    internal val cNamesStructs = cNames.child(Name.identifier("structs"))

    private val objCNames = FqName("objcnames")
    internal val objCNamesClasses = objCNames.child(Name.identifier("classes"))
    internal val objCNamesProtocols = objCNames.child(Name.identifier("protocols"))

    val syntheticPackages = setOf(cNames, objCNames)
}
