/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.Printer

// TODO: eliminate Native specifics.
class KlibResolvedModuleDescriptorsFactoryImpl(
    override val moduleDescriptorFactory: KlibMetadataModuleDescriptorFactory
) : KlibResolvedModuleDescriptorsFactory {

    override fun createResolved(
        resolvedLibraries: KotlinLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        languageVersionSettings: LanguageVersionSettings,
        friendModuleFiles: Set<File>,
        refinesModuleFiles: Set<File>,
        includedLibraryFiles: Set<File>,
        additionalDependencyModules: Iterable<ModuleDescriptorImpl>,
        isForMetadataCompilation: Boolean,
    ): KotlinResolvedModuleDescriptors {

        val moduleDescriptors = mutableListOf<ModuleDescriptorImpl>()

        @Suppress("NAME_SHADOWING")
        var builtIns = builtIns

        val friendModuleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        val refinesModuleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
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

                if (refinesModuleFiles.contains(library.libraryFile))
                    refinesModuleDescriptors.add(moduleDescriptor)
                if (friendModuleFiles.contains(library.libraryFile))
                    friendModuleDescriptors.add(moduleDescriptor)
                if (includedLibraryFiles.contains(library.libraryFile))
                    includedLibraryDescriptors.add(moduleDescriptor)
            }
        }

        val forwardDeclarationsModule = createForwardDeclarationsModule(
            builtIns,
            storageManager,
            // If we are compiling metadata, make synthetic forward declarations `expect`,
            // because otherwise `getFirstClassifierDiscriminateHeaders` would prefer it over a
            // "real" `expect` declaration from a commonized interop library, which would ruin
            // the whole idea of using synthetic forward declarations only when no proper definitions
            // are found.
            //
            // If we are compiling for the actual native platform, continue using non-expect
            // forward declarations (to prevent getting non-actualized expects into the backend,
            // and to prevent related klib signature changes).
            isExpect = isForMetadataCompilation,
        )

        // Set inter-dependencies between module descriptors, add forwarding declarations module.
        val additionalDependencyModulesCopy = additionalDependencyModules.toSet()
        val friendsForNonIncludedModule = additionalDependencyModulesCopy
        val friendsForIncludedModule = buildSet<ModuleDescriptorImpl> {
            this += friendsForNonIncludedModule
            this += friendModuleDescriptors
            this += refinesModuleDescriptors
        }
        val allDependencies = moduleDescriptors + additionalDependencyModulesCopy + forwardDeclarationsModule
        for (module in moduleDescriptors) {
            val friends = if (module in includedLibraryDescriptors) {
                friendsForIncludedModule
            } else {
                friendsForNonIncludedModule
            }

            // Yes, just to all of them.
            module.setDependencies(allDependencies, friends)
        }

        return KotlinResolvedModuleDescriptors(
            resolvedDescriptors = moduleDescriptors,
            forwardDeclarationsModule = forwardDeclarationsModule,
            friendModules = friendModuleDescriptors,
            refinesModules = refinesModuleDescriptors
        )
    }

    fun createForwardDeclarationsModule(
        builtIns: KotlinBuiltIns?,
        storageManager: StorageManager,
        isExpect: Boolean
    ): ModuleDescriptorImpl {

        val module = createDescriptorOptionalBuiltsIns(FORWARD_DECLARATIONS_MODULE_NAME, storageManager, builtIns, SyntheticModulesOrigin)

        fun createPackage(forwardDeclarationKind: NativeForwardDeclarationKind) =
            ForwardDeclarationsPackageFragmentDescriptor(
                storageManager,
                module,
                forwardDeclarationKind.packageFqName,
                forwardDeclarationKind.superClassName,
                forwardDeclarationKind.classKind,
                isExpect
            )

        val packageFragmentProvider = PackageFragmentProviderImpl(
            NativeForwardDeclarationKind.entries.map { createPackage(it) }
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
    classKind: ClassKind,
    isExpect: Boolean
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val declarations = storageManager.createMemoizedFunction(this::createDeclaration)

        private val supertype by storageManager.createLazyValue {
            findCinteropClass(supertypeName).defaultType
        }

        /**
         * Normally, this can't be null. But it's possible inside IDE, if one uses new IDE with
         * old compiler in a project. In that case, IDE would try to generate synthetic declaration
         * but would fail to find annotation class.
         *
         * A better way to do this would be introducing language feature, but unfortunately, we can't
         * do it between 1.9.0 and 1.9.20.
         */
        private val experimentalAnnotationType by storageManager.createNullableLazyValue {
            findCinteropClassOrNull(NativeStandardInteropNames.ExperimentalForeignApi)?.defaultType
        }

        private fun findCinteropClass(name: Name): ClassDescriptor = findCinteropClassOrNull(name) ?:
          error("Class $name is not found")

        private fun findCinteropClassOrNull(name: Name): ClassDescriptor? {
            return builtIns.builtInsModule.getPackage(NativeStandardInteropNames.cInteropPackage)
                .memberScope
                .getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor?
        }

        private fun createDeclaration(name: Name): ClassDescriptor {
            val experimentalAnnotation = experimentalAnnotationType?.let {
                AnnotationDescriptorImpl(
                    it,
                    emptyMap(),
                    SourceElement.NO_SOURCE
                )
            }

            return object : ClassDescriptorImpl(
                this@ForwardDeclarationsPackageFragmentDescriptor,
                name,
                Modality.FINAL,
                classKind,
                listOf(supertype),
                SourceElement.NO_SOURCE,
                false,
                LockBasedStorageManager.NO_LOCKS
            ) {
                override fun isExpect(): Boolean = isExpect
                override val annotations: Annotations = Annotations.create(listOfNotNull(experimentalAnnotation))
            }.apply {
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


@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This class was moved to org.jetbrains.kotlin.name.NativeStandardInteropNames.ForwardDeclarations",
)
object ForwardDeclarationsFqNames {

    internal val cInterop = NativeStandardInteropNames.cInteropPackage

    internal val cNamesStructs = NativeStandardInteropNames.ForwardDeclarations.cNamesStructsPackage
    internal val objCNamesClasses = NativeStandardInteropNames.ForwardDeclarations.objCNamesClassesPackage
    internal val objCNamesProtocols = NativeStandardInteropNames.ForwardDeclarations.objCNamesProtocolsPackage

    val syntheticPackages = NativeStandardInteropNames.ForwardDeclarations.syntheticPackages
}