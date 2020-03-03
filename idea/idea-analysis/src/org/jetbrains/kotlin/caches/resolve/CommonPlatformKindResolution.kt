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
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.resolve.BuiltInsCacheKey
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.klib.getCompatibilityInfo
import org.jetbrains.kotlin.idea.util.IJLoggerAdapter
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import java.io.IOException
import java.util.*
import org.jetbrains.kotlin.konan.file.File as KFile

class CommonPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION || virtualFile.isMetadataKlib
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = CommonLibraryKind

    override val kind get() = CommonIdePlatformKind

    override fun getKeyForBuiltIns(moduleInfo: ModuleInfo, sdkInfo: SdkInfo?): BuiltInsCacheKey = BuiltInsCacheKey.DefaultBuiltInsKey

    override fun createBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext, sdkDependency: SdkInfo?): KotlinBuiltIns {
        return DefaultBuiltIns.Instance
    }

    override fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        val klibFiles = library.getFiles(OrderRootType.CLASSES).filter { it.isMetadataKlib }

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
}

// TODO(dsavvinov): unify with NativeLibraryInfo
class CommonKlibLibraryInfo(project: Project, library: Library, val libraryRoot: String) : LibraryInfo(project, library) {

    val commonLibrary = resolveSingleFileKlib(
        libraryFile = KFile(libraryRoot),
        logger = LOG,
        strategy = ToolingSingleFileKlibResolveStrategy
    )

    val compatibilityInfo by lazy { commonLibrary.getCompatibilityInfo() }

    override fun getLibraryRoots() = listOf(libraryRoot)

    override val platform: TargetPlatform
        get() = CommonPlatforms.defaultCommonPlatform

    override fun toString() = "CommonKlib" + super.toString()

    companion object {
        private val LOG = IJLoggerAdapter.getInstance(CommonKlibLibraryInfo::class.java)
    }
}

val VirtualFile.isMetadataKlib: Boolean
    get() {
        val extension = extension
        if (!extension.isNullOrEmpty() && extension != KLIB_FILE_EXTENSION) return false

        fun checkComponent(componentFile: VirtualFile): Boolean {
            val manifestFile = componentFile.findChild(KLIB_MANIFEST_FILE_NAME)?.takeIf { !it.isDirectory } ?: return false

            val manifestProperties = try {
                manifestFile.inputStream.use { Properties().apply { load(it) } }
            } catch (_: IOException) {
                return false
            }

            if (!manifestProperties.containsKey(KLIB_PROPERTY_UNIQUE_NAME)) return false

            // No builtins_platform property => either a new common klib (we don't write builtins_platform for common) or old Native klib
            return manifestProperties.getProperty(KLIB_PROPERTY_BUILTINS_PLATFORM) == null && !componentFile.isLegacyNativeKlibComponent
        }

        // run check for library root too
        // this is necessary to recognize old style KLIBs that do not have components, and report them to user appropriately
        return checkComponent(this) || children?.any(::checkComponent) == true
    }

private val VirtualFile.isLegacyNativeKlibComponent: Boolean
    get() {
        val irFolder = findChild(KLIB_IR_FOLDER_NAME)
        return irFolder != null && irFolder.children.isNotEmpty()
    }
