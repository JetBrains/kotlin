/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.ide.konan.analyzer.NativeAnalyzerFacade
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform

class NativePlatformKindResolution : IdePlatformKindResolution {

    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return if (virtualFile.isDirectory) {
            virtualFile.findChild("linkdata")?.takeIf { it.isDirectory }
                ?.children?.any { it.extension == KLIB_METADATA_FILE_EXTENSION } == true
        } else {
            virtualFile.extension == KLIB_FILE_EXTENSION
        }
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = NativeLibraryKind

    override val kind get() = NativeIdePlatformKind

    override val resolverForModuleFactory get() = NativeAnalyzerFacade

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext) =
        createKotlinNativeBuiltIns(projectContext)
}

private fun createKotlinNativeBuiltIns(projectContext: ProjectContext): KotlinBuiltIns {

    // TODO: It depends on a random project's stdlib, propagate the actual project here.
    fun findStdlib(): Pair<String, LibraryInfo>? {
        getModuleInfosFromIdeaModel(projectContext.project, KonanPlatform).forEach { module ->
            module.dependencies().forEach { dependency ->
                (dependency as? LibraryInfo)?.getLibraryRoots()?.forEach { path ->
                    if (path.endsWith(KONAN_STDLIB_NAME)) {
                        return path to dependency
                    }
                }
            }
        }
        return null
    }

    val stdlib: Pair<String, LibraryInfo>? = findStdlib()

    if (stdlib != null) {

        val (path, libraryInfo) = stdlib
        val library = createKonanLibrary(
            File(path),
            KOTLIN_NATIVE_CURRENT_ABI_VERSION,
            metadataReader = CachingIdeMetadataReaderImpl
        )

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
