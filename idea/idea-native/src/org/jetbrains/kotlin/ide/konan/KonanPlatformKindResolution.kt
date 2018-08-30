/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.io.exists
import org.jetbrains.konan.KONAN_CURRENT_ABI_VERSION
import org.jetbrains.konan.analyser.KonanAnalyzerFacade
import org.jetbrains.konan.settings.KonanPaths
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultDeserializedDescriptorFactory

class KonanPlatformKindResolution : IdePlatformKindResolution {

    override val kind get() = KonanPlatformKind

    override val resolverForModuleFactory get() = KonanAnalyzerFacade

    override fun isModuleForPlatform(module: Module) =
        KotlinFacetSettingsProvider.getInstance(module.project).getInitializedSettings(module).platformKind.isKonan

    override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl) = createKonanBuiltIns(sdkContext)
}

private fun createKonanBuiltIns(sdkContext: GlobalContextImpl): KotlinBuiltIns {

    // TODO: it depends on a random project's stdlib, propagate the actual project here
    val stdlibPath = ProjectManager.getInstance().openProjects.asSequence().mapNotNull { project ->
        KonanPaths.getInstance(project).konanStdlib()?.takeIf { it.exists() }
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
