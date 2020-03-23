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
import org.jetbrains.kotlin.idea.klib.AbstractKlibLibraryInfo
import org.jetbrains.kotlin.idea.klib.isKlibLibraryRootForPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.resolve.TargetEnvironment

class JsPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == "js"
                || virtualFile.extension == "kjsm"
                || virtualFile.isKlibLibraryRootForPlatform(JsPlatforms.defaultJsPlatform)
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
        val klibFiles = library.getFiles(OrderRootType.CLASSES).filter {
            it.isKlibLibraryRootForPlatform(JsPlatforms.defaultJsPlatform)
        }

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

class JsKlibLibraryInfo(project: Project, library: Library, libraryRoot: String) : AbstractKlibLibraryInfo(project, library, libraryRoot) {
    override val platform: TargetPlatform
        get() = JsPlatforms.defaultJsPlatform
}
