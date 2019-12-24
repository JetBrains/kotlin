/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.PlatformAnalysisParameters
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.analyzer.getCapability
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.ide.konan.NativeLibraryInfo.Companion.safeMetadataVersion
import org.jetbrains.kotlin.ide.konan.NativeLibraryInfo.MetadataInfo.Compatible
import org.jetbrains.kotlin.ide.konan.NativeLibraryInfo.MetadataInfo.Incompatible
import org.jetbrains.kotlin.ide.konan.analyzer.NativeResolverForModuleFactory
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.project.lazyClosure
import org.jetbrains.kotlin.idea.caches.resolve.BuiltInsCacheKey
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.KonanFactories
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager
import java.io.IOException

fun KotlinLibrary.createPackageFragmentProvider(
    storageManager: StorageManager,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor
): PackageFragmentProvider? {

    if (safeMetadataVersion?.isCompatible() != true) return null

    val libraryProto = CachingIdeKonanLibraryMetadataLoader.loadModuleHeader(this)

    val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    return KonanFactories.DefaultDeserializedDescriptorFactory.createPackageFragmentProvider(
        this,
        CachingIdeKonanLibraryMetadataLoader,
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
            NativeLibraryInfo(project, library, path)
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

    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean = virtualFile.isKonanLibraryRoot

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

    override fun getKeyForBuiltIns(moduleInfo: ModuleInfo, sdkInfo: SdkInfo?): BuiltInsCacheKey = NativeBuiltInsCacheKey

    override fun createBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext, sdkDependency: SdkInfo?) =
        createKotlinNativeBuiltIns(moduleInfo, projectContext)

    object NativeBuiltInsCacheKey : BuiltInsCacheKey
}

private fun createKotlinNativeBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext): KotlinBuiltIns {

    val project = projectContext.project
    val storageManager = projectContext.storageManager

    val stdlibInfo = moduleInfo.findNativeStdlib() ?: return DefaultBuiltIns.Instance
    val konanLibrary = stdlibInfo.getCapability(NativeLibraryInfo.NATIVE_LIBRARY_CAPABILITY)!!

    val builtInsModule = KonanFactories.DefaultDescriptorFactory.createDescriptorAndNewBuiltIns(
        KotlinBuiltIns.BUILTINS_MODULE_NAME,
        storageManager,
        DeserializedKlibModuleOrigin(konanLibrary),
        stdlibInfo.capabilities
    )

    val languageSettings = IDELanguageSettingsProvider.getLanguageVersionSettings(stdlibInfo, project, isReleaseCoroutines = false)
    val deserializationConfiguration = CompilerDeserializationConfiguration(languageSettings)

    val libraryProto = CachingIdeKonanLibraryMetadataLoader.loadModuleHeader(konanLibrary)

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
                functionInterfacePackageFragmentProvider(storageManager, builtInsModule),
                (KonanFactories.DefaultDeserializedDescriptorFactory as KlibMetadataModuleDescriptorFactoryImpl)
                    .createForwardDeclarationHackPackagePartProvider(storageManager, builtInsModule)
            )
        )
    )

    builtInsModule.setDependencies(listOf(builtInsModule))

    return builtInsModule.builtIns
}

private fun ModuleInfo.findNativeStdlib(): NativeLibraryInfo? =
    dependencies().lazyClosure { it.dependencies() }
        .filterIsInstance<NativeLibraryInfo>()
        .firstOrNull { it.isStdlib && it.metadataInfo.isCompatible }

class NativeLibraryInfo(project: Project, library: Library, val libraryRoot: String) : LibraryInfo(project, library) {

    sealed class MetadataInfo {
        abstract val isCompatible: Boolean

        object Compatible : MetadataInfo() {
            override val isCompatible get() = true
        }

        class Incompatible(val isOlder: Boolean) : MetadataInfo() {
            override val isCompatible get() = false
        }
    }

    private val nativeLibrary = createKotlinLibrary(File(libraryRoot))

    val isStdlib get() = libraryRoot.endsWith(KONAN_STDLIB_NAME)
    val metadataInfo by lazy {
        val metadataVersion = nativeLibrary.safeMetadataVersion
        when {
            metadataVersion == null -> Incompatible(true) // too old KLIB format, even doesn't have metadata version
            !metadataVersion.isCompatible() -> Incompatible(!metadataVersion.isAtLeast(KlibMetadataVersion.INSTANCE))
            else -> Compatible
        }
    }

    override fun getLibraryRoots() = listOf(libraryRoot)

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() {
            val capabilities = super.capabilities.toMutableMap()
            capabilities += KlibModuleOrigin.CAPABILITY to DeserializedKlibModuleOrigin(nativeLibrary)
            capabilities += NATIVE_LIBRARY_CAPABILITY to nativeLibrary
            capabilities += ImplicitIntegerCoercion.MODULE_CAPABILITY to nativeLibrary.readSafe(false) { isInterop }
            return capabilities
        }

    override val platform: TargetPlatform
        get() = KonanPlatforms.defaultKonanPlatform

    override fun toString() = "Native" + super.toString()

    companion object {
        val NATIVE_LIBRARY_CAPABILITY = ModuleDescriptor.Capability<KotlinLibrary>("KotlinNativeLibrary")

        internal val KotlinLibrary.safeMetadataVersion get() = this.readSafe(null) { metadataVersion }

        private fun <T> KotlinLibrary.readSafe(defaultValue: T, action: KotlinLibrary.() -> T) = try {
            action()
        } catch (_: IOException) {
            defaultValue
        }
    }
}
