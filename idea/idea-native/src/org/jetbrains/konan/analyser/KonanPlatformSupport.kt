/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.analyser

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.util.io.exists
import org.jetbrains.konan.KONAN_CURRENT_ABI_VERSION
import org.jetbrains.konan.settings.KonanPaths
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformSupport
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform

/**
 * @author Alefas
 */
class KonanPlatformSupport : IdePlatformSupport() {

    override val resolverForModuleFactory: ResolverForModuleFactory
        get() = KonanAnalyzerFacade()

    override val libraryKind: PersistentLibraryKind<*>? = null

    override val platform: TargetPlatform
        get() = KonanPlatform

    override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl) = createKonanBuiltIns(sdkContext)

    override fun isModuleForPlatform(module: Module) = true
}

private fun createKonanBuiltIns(sdkContext: GlobalContextImpl): KotlinBuiltIns {

    // TODO: it depends on a random project's stdlib, propagate the actual project here
    val stdlibPath = ProjectManager.getInstance().openProjects.asSequence().mapNotNull {
        KonanPaths.getInstance(it).konanStdlib()?.takeIf { it.exists() }
    }.firstOrNull()

    if (stdlibPath != null) {
        val library = createKonanLibrary(stdlibPath.File(), KONAN_CURRENT_ABI_VERSION)

        val builtInsModule = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library,
            LanguageVersionSettingsImpl.DEFAULT,
            sdkContext.storageManager
        )
        builtInsModule.setDependencies(listOf(builtInsModule))

        return builtInsModule.builtIns
    }

    return DefaultBuiltIns.Instance
}
