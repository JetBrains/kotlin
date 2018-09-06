/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.konan.KONAN_CURRENT_ABI_VERSION
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.ide.konan.analyzer.NativeAnalyzerFacade
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.util.KonanFactories
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.storage.StorageManager

class NativePlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == "klib"
                || virtualFile.isDirectory && virtualFile.children.any { it.name == "manifest" }
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = NativeLibraryKind

    override val kind get() = NativeIdePlatformKind

    override val resolverForModuleFactory get() = NativeAnalyzerFacade

    override fun isModuleForPlatform(module: Module) = module.isKotlinNativeModule

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext) =
        createKotlinNativeBuiltIns(projectContext)
}

val Module.isKotlinNativeModule: Boolean
    get() {
        val settings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this)
        return settings.platformKind.isKotlinNative
    }

val KonanFactoriesKey = Key<MutableMap<StorageManager, KonanFactories>>("KonanFactoriesKey")

var Project.KonanFactories by UserDataProperty(KonanFactoriesKey)

fun Project.getOrCreateKonanFactories(storageManager: StorageManager): KonanFactories {
    val map = this.KonanFactories ?: ContainerUtil.createConcurrentWeakKeySoftValueMap<StorageManager, KonanFactories>().also {
        this.KonanFactories = it
    }

    return map.getOrPut(storageManager) {
        KonanFactories(storageManager)
    }
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
        val library = createKonanLibrary(File(path), KONAN_CURRENT_ABI_VERSION)
        val KonanFactories = projectContext.project.getOrCreateKonanFactories(projectContext.storageManager)


        val builtInsModule = KonanFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
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
