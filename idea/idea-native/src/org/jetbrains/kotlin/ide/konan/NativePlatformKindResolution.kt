/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.PlatformAnalysisParameters
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.analyzer.getCapability
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.ide.konan.analyzer.NativeResolverForModuleFactory
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.lazyClosure
import org.jetbrains.kotlin.idea.caches.resolve.BuiltInsCacheKey
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager

fun KotlinLibrary.createPackageFragmentProvider(
        storageManager: StorageManager,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptor
    ): PackageFragmentProvider {

    val library = this

    val libraryProto = parseModuleHeader(library.moduleHeaderData)

    //TODO: Is it required somehow?
    //val moduleName = Name.special(libraryProto.moduleName)
    //val moduleOrigin = DeserializedKonanModuleOrigin(library)

    val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    return KonanFactories.DefaultDeserializedDescriptorFactory.createPackageFragmentProvider(
        library,
        null,
        libraryProto.packageFragmentNameList,
        storageManager,
        moduleDescriptor,
        deserializationConfiguration,
        null
    )
}

class NativePlatformKindResolution : IdePlatformKindResolution {

    override fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        return library.getFiles(OrderRootType.CLASSES).mapNotNull { file ->
            if (!isLibraryFileForPlatform(file)) return@createLibraryInfo emptyList()
            val path = PathUtil.getLocalPath(file) ?: return@createLibraryInfo emptyList()
            NativeLibraryInfo(project, library, File(path))
        }
    }

    override fun createPlatformSpecificPackageFragmentProvider(
        moduleInfo: ModuleInfo,
        storageManager: StorageManager,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptor
    ): PackageFragmentProvider? {
        val konanLibrary = moduleInfo.getCapability(NativeLibraryInfo.NATIVE_LIBRARY_CAPABILITY) ?: return null
        return konanLibrary.createPackageFragmentProvider(
            storageManager,
            languageVersionSettings,
            moduleDescriptor
        )
    }

    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return when {
            // The virtual file for a library packed in a ZIP file will have path like "/some/path/to/the/file.klib!/",
            // and therefore will be recognized by VFS as a directory (isDirectory == true).
            // So, first, let's check the extension.
            virtualFile.extension == KLIB_FILE_EXTENSION -> true

            virtualFile.isDirectory -> {
                val linkdataDir = virtualFile.findChild("linkdata") ?: return false
                // False means we hit .knm file
                !VfsUtil.processFilesRecursively(linkdataDir) {
                    it.extension != KLIB_METADATA_FILE_EXTENSION
                }
            }

            else -> false
        }
    }

    override fun createResolverForModuleFactory(
        settings: PlatformAnalysisParameters,
        environment: TargetEnvironment,
        platform: TargetPlatform
    ): ResolverForModuleFactory {
        return NativeResolverForModuleFactory(settings, environment, platform)
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = NativeLibraryKind

    override val kind get() = NativeIdePlatformKind

    override fun getKeyForBuiltIns(moduleInfo: ModuleInfo): BuiltInsCacheKey = NativeBuiltInsCacheKey

    override fun createBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext) =
        createKotlinNativeBuiltIns(moduleInfo, projectContext)

    object NativeBuiltInsCacheKey : BuiltInsCacheKey
}

private fun createKotlinNativeBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext): KotlinBuiltIns {

    val project = projectContext.project
    val storageManager = projectContext.storageManager

    val stdlibInfo = moduleInfo.findNativeStdlib(project) ?: return DefaultBuiltIns.Instance
    val konanLibrary = stdlibInfo.getCapability(NativeLibraryInfo.NATIVE_LIBRARY_CAPABILITY)!!

    val builtInsModule = KonanFactories.DefaultDescriptorFactory.createDescriptorAndNewBuiltIns(
        KotlinBuiltIns.BUILTINS_MODULE_NAME,
        storageManager,
        DeserializedKlibModuleOrigin(konanLibrary),
        stdlibInfo.capabilities
    )

    val languageSettings = IDELanguageSettingsProvider.getLanguageVersionSettings(stdlibInfo, project, isReleaseCoroutines = false)
    val deserializationConfiguration = CompilerDeserializationConfiguration(languageSettings)

    val libraryProto = parseModuleHeader(konanLibrary.moduleHeaderData)

    val stdlibFragmentProvider = KonanFactories.DefaultDeserializedDescriptorFactory.createPackageFragmentProvider(
        konanLibrary,
        CachingIdeKonanLibraryMetadataLoader,
        libraryProto.packageFragmentNameList,
        storageManager,
        builtInsModule,
        deserializationConfiguration,
        null
    )

    builtInsModule.initialize(
        CompositePackageFragmentProvider(
            listOf(
                stdlibFragmentProvider,
                (KonanFactories.DefaultDeserializedDescriptorFactory as KlibMetadataModuleDescriptorFactoryImpl)
                    .createForwardDeclarationHackPackagePartProvider(
                        storageManager,
                        builtInsModule
                    )
            )
        )
    )

    builtInsModule.setDependencies(listOf(builtInsModule))

    return builtInsModule.builtIns
}

private fun ModuleInfo.findNativeStdlib(project: Project): NativeLibraryInfo? =
    dependencies().lazyClosure { it.dependencies() }
        .filterIsInstance<NativeLibraryInfo>()
        .firstOrNull { it.isStdlib }

class NativeLibraryInfo(project: Project, library: Library, root: File) : LibraryInfo(project, library) {

    private val nativeLibrary = createKotlinLibrary(root)

    private val roots = listOf(root.absolutePath)

    val isStdlib by lazy { roots.first().endsWith(KONAN_STDLIB_NAME) }

    override fun getLibraryRoots() = roots

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = super.capabilities +
                mapOf(
                    KlibModuleOrigin.CAPABILITY to this,
                    ImplicitIntegerCoercion.MODULE_CAPABILITY to nativeLibrary.isInterop
                )

    override val platform: TargetPlatform
        get() = KonanPlatforms.defaultKonanPlatform

    override fun toString() = "Native" + super.toString()

    companion object {
        val NATIVE_LIBRARY_CAPABILITY = ModuleDescriptor.Capability<KotlinLibrary>("KotlinNativeLibrary")
    }
}
