/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.resolve.BuiltInsCacheKey
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.resolve.TargetEnvironment
import java.io.IOException
import java.util.*

class JsPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == "js" || virtualFile.extension == "kjsm" || virtualFile.isJsKlibRoot
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = JSLibraryKind

    override val kind get() = JsIdePlatformKind

    override fun getKeyForBuiltIns(moduleInfo: ModuleInfo, sdkInfo: SdkInfo?): BuiltInsCacheKey {
        return BuiltInsCacheKey.DefaultBuiltInsKey
    }

    override fun createBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext, sdkDependency: SdkInfo?): KotlinBuiltIns {
        return DefaultBuiltIns.Instance
    }

    override fun createResolverForModuleFactory(
        settings: PlatformAnalysisParameters,
        environment: TargetEnvironment,
        platform: TargetPlatform
    ): ResolverForModuleFactory = JsResolverForModuleFactory(environment)

    override fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        val klibFiles = library.getFiles(OrderRootType.CLASSES).filter { it.isJsKlibRoot }

        return if (klibFiles.isNotEmpty()) {
            klibFiles.mapNotNull {
                // TODO report error?
                val path = PathUtil.getLocalPath(it) ?: return@mapNotNull null
                JsKlibLibraryInfo(project, library, path)
            }
        } else {
            super.createLibraryInfo(project, library)
        }
    }
}

// TODO unify the solution between Native and Common
private val VirtualFile.isJsKlibRoot: Boolean
    get() {
        // The virtual file for a library packed in a ZIP file will have path like "/some/path/to/the/file.klib!/",
        // and therefore will be recognized by VFS as a directory (isDirectory == true).
        // So, first, let's check the extension.
        val extension = extension
        if (!extension.isNullOrEmpty() && extension != KLIB_FILE_EXTENSION) return false

        fun checkComponent(componentFile: VirtualFile): Boolean {
            val manifestFile = componentFile.findChild(KLIB_MANIFEST_FILE_NAME)?.takeIf { !it.isDirectory } ?: return false

            val manifestProperties = try {
                manifestFile.inputStream.use { Properties().apply { load(it) } }
            } catch (_: IOException) {
                return false
            }

            return manifestProperties.getProperty(KLIB_PROPERTY_BUILTINS_PLATFORM) == BuiltInsPlatform.JS.name
        }

        // run check for library root too
        // this is necessary to recognize old style KLIBs that do not have components, and report tem to user appropriately
        return checkComponent(this) || children?.any(::checkComponent) == true
    }
