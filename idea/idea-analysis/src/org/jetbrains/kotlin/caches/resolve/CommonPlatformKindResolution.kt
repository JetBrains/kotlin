/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.PlatformAnalysisParameters
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.resolve.BuiltInsCacheKey
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.klib.AbstractKlibLibraryInfo
import org.jetbrains.kotlin.idea.klib.createKlibPackageFragmentProvider
import org.jetbrains.kotlin.idea.klib.isKlibLibraryRootForPlatform
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager

class CommonPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION ||
                virtualFile.isKlibLibraryRootForPlatform(CommonPlatforms.defaultCommonPlatform)
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = CommonLibraryKind

    override val kind get() = CommonIdePlatformKind

    override fun getKeyForBuiltIns(moduleInfo: ModuleInfo, sdkInfo: SdkInfo?): BuiltInsCacheKey = BuiltInsCacheKey.DefaultBuiltInsKey

    override fun createBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext, sdkDependency: SdkInfo?): KotlinBuiltIns {
        return DefaultBuiltIns.Instance
    }

    override fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        val klibFiles = library.getFiles(OrderRootType.CLASSES).filter {
            it.isKlibLibraryRootForPlatform(CommonPlatforms.defaultCommonPlatform)
        }

        return if (klibFiles.isNotEmpty()) {
            klibFiles.mapNotNull {
                val path = PathUtil.getLocalPath(it) ?: return@mapNotNull null
                CommonKlibLibraryInfo(project, library, path)
            }
        } else {
            // No klib files <=> old metadata-library <=> create usual LibraryInfo
            listOf(LibraryInfo(project, library))
        }
    }

    override fun createKlibPackageFragmentProvider(
        moduleInfo: ModuleInfo,
        storageManager: StorageManager,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptor
    ): PackageFragmentProvider? {
        return (moduleInfo as? CommonKlibLibraryInfo)
            ?.resolvedKotlinLibrary
            ?.createKlibPackageFragmentProvider(
                storageManager = storageManager,
                metadataModuleDescriptorFactory = metadataModuleDescriptorFactory,
                languageVersionSettings = languageVersionSettings,
                moduleDescriptor = moduleDescriptor,
                lookupTracker = LookupTracker.DO_NOTHING
            )
    }

    override fun createResolverForModuleFactory(
        settings: PlatformAnalysisParameters,
        environment: TargetEnvironment,
        platform: TargetPlatform
    ): ResolverForModuleFactory {
        return CommonResolverForModuleFactory(
            settings as CommonAnalysisParameters,
            environment,
            platform,
            shouldCheckExpectActual = true
        )
    }

    companion object {
        private val metadataFactories = KlibMetadataFactories({ DefaultBuiltIns.Instance }, NullFlexibleTypeDeserializer)

        private val metadataModuleDescriptorFactory = KlibMetadataModuleDescriptorFactoryImpl(
            metadataFactories.DefaultDescriptorFactory,
            metadataFactories.DefaultPackageFragmentsFactory,
            metadataFactories.flexibleTypeDeserializer,
            metadataFactories.platformDependentTypeTransformer
        )
    }
}

class CommonKlibLibraryInfo(project: Project, library: Library, libraryRoot: String) :
    AbstractKlibLibraryInfo(project, library, libraryRoot) {

    override val platform: TargetPlatform
        get() = CommonPlatforms.defaultCommonPlatform
}
