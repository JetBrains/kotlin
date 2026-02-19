/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

/**
 * [KotlinProjectStructureProvider] provides information about the project's structure as managed by the Analysis API platform.
 *
 * The project structure provider must only provide a [KaModule] for files/PSI elements which are included in that module's
 * [content scope][KaModule.contentScope]. Implementations of [KotlinProjectStructureProvider] are encouraged to check containing files
 * against the content scopes of candidate [KaModule]s.
 *
 * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider
 */
@KaPlatformInterface
public interface KotlinProjectStructureProvider {
    /**
     * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider.getModule
     */
    public fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule

    public fun getImplementingModules(module: KaModule): List<KaModule>

    /**
     * Project-global [LanguageVersionSettings] for source modules lacking explicit settings (such as [KaNotUnderContentRootModule]).
     */
    public val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * Project-global [LanguageVersionSettings] for [KaLibraryModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule]s
     * and [KaLibrarySourceModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule]s.
     */
    public val libraryLanguageVersionSettings: LanguageVersionSettings
        get() = globalLanguageVersionSettings

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KotlinProjectStructureProvider = project.service()

        public fun getModule(project: Project, element: PsiElement, useSiteModule: KaModule?): KaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
