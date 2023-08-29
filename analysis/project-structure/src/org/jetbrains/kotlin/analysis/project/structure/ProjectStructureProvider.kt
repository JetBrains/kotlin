/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

public abstract class ProjectStructureProvider {
    /**
     * Returns a [KtModule] for a given [element] in context of the [contextualModule].
     *
     * Normally, every Kotlin source file either belongs to some module (e.g. a source module, or a library module), or is self-contained
     * (a script file, or a file outside content roots). However, in certain cases there might be special modules that include both
     * existing source files, and also some additional files.
     *
     * An example of such a module is one that owns an 'outsider' source file. Outsiders are used in IntelliJ for displaying files that
     * technically belong to some module, but are not included in the module's content roots (e.g. a file from previous VCS revision).
     * As there might be cross-references between the outsider file and other files in the module, they need to be analyzed as a single
     * synthetic module. Inside an analysis session for such a module (that would become there a 'contextualModule'),
     * sources that originally belong to a source module should be treated rather as a part of the synthetic one.
     */
    public abstract fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule

    /**
     * Project-global [LanguageVersionSettings] for source modules lacking explicit settings (such as [KtNotUnderContentRootModule]).
     */
    public open val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * Project-global [LanguageVersionSettings] for [KtLibraryModule]s and [KtLibrarySourceModule]s.
     */
    public open val libraryLanguageVersionSettings: LanguageVersionSettings
        get() = globalLanguageVersionSettings

    public companion object {
        public fun getInstance(project: Project): ProjectStructureProvider {
            return project.getService(ProjectStructureProvider::class.java)
        }

        public fun getModule(project: Project, element: PsiElement, contextualModule: KtModule?): KtModule {
            return getInstance(project).getModule(element, contextualModule)
        }
    }
}