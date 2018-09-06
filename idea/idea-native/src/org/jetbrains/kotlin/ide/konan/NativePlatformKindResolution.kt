/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import org.jetbrains.konan.KONAN_CURRENT_ABI_VERSION
import org.jetbrains.kotlin.ide.konan.analyzer.NativeAnalyzerFacade
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultDeserializedDescriptorFactory
import java.nio.file.Path
import java.nio.file.Paths

class NativePlatformKindResolution : IdePlatformKindResolution {

    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == "klib"
                || virtualFile.isDirectory && virtualFile.children.any { it.name == "manifest" }
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = NativeLibraryKind

    override val kind get() = NativeIdePlatformKind

    override val resolverForModuleFactory get() = NativeAnalyzerFacade

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext) =
        createKotlinNativeBuiltIns(projectContext)
}

val Module.isKotlinNativeModule: Boolean
    get() {
        val settings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this)
        return settings.platform.isKotlinNative
    }

private fun createKotlinNativeBuiltIns(projectContext: ProjectContext): KotlinBuiltIns {

    // TODO: It depends on a random project's stdlib, propagate the actual project here.
    val stdlib: Pair<Path, LibraryInfo>? = ProjectManager.getInstance().openProjects.asSequence().mapNotNull { project ->

        ModuleManager.getInstance(project).modules.asSequence().filter { it.isKotlinNativeModule }.mapNotNull { module ->

            var result: Pair<Path, LibraryInfo>? = null

            ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library ->
                if (library.name == KONAN_STDLIB_NAME) {
                    val libraryInfo = LibraryInfo(project, library)
                    val path = libraryInfo.getLibraryRoots().firstOrNull()?.let { Paths.get(it) }?.takeIf { it.exists() }
                    if (path != null) result = path to libraryInfo
                }

                result == null
            }
            result
        }
    }.flatten().firstOrNull()

    if (stdlib != null) {

        val (path, libraryInfo) = stdlib
        val library = createKonanLibrary(path.File(), KONAN_CURRENT_ABI_VERSION)

        val builtInsModule = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library,
            LanguageVersionSettingsImpl.DEFAULT,
            projectContext.storageManager,
            // This is to preserve "capabilities" from the original IntelliJ LibraryInfo:
            customCapabilities = libraryInfo.capabilities
        )
        builtInsModule.setDependencies(listOf(builtInsModule))

        return builtInsModule.builtIns
    }

    return DefaultBuiltIns.Instance
}
