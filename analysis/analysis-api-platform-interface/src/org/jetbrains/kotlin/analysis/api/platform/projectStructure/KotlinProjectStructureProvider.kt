/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

/**
 * [KotlinProjectStructureProvider] provides information about the project's structure as managed by the Analysis API platform.
 */
public interface KotlinProjectStructureProvider {
    /**
     * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider.getModule
     */
    public fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule

    /**
     * Project-global [LanguageVersionSettings] for source modules lacking explicit settings (such as [KaNotUnderContentRootModule]).
     */
    public val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * Project-global [LanguageVersionSettings] for [KtLibraryModule]s and [KtLibrarySourceModule]s.
     */
    public val libraryLanguageVersionSettings: LanguageVersionSettings
        get() = globalLanguageVersionSettings

    public companion object {
        public fun getInstance(project: Project): KotlinProjectStructureProvider = project.service()

        public fun getModule(project: Project, element: PsiElement, useSiteModule: KaModule?): KaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
