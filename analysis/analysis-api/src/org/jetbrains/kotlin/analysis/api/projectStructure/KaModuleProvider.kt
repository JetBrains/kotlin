/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public interface KaModuleProvider {
    /**
     * Returns a [KtModule] for a given [element] in the context of the [useSiteModule].
     *
     * The use-site module is the [KtModule] from which [getModule] is called. This concept is the same as the use-site module accepted by
     * [analyze][org.jetbrains.kotlin.analysis.api.analyze], and closely related to the concept of a use-site element. In essence, when we
     * are performing analysis, most of the time we do so from the point of view of a particular [KtModule] or [PsiElement]. If this module
     * is already known, it should be passed as the [useSiteModule] to [getModule].
     *
     * Here, the use-site module is a way to disambiguate the [KtModule] of [element]s with whom multiple modules might be associated:
     *
     *  1. It allows replacing the original [KtModule] of [element] with another module, e.g. for supporting outsider files (see below).
     *  2. It helps to distinguish between multiple possible [KtModule]s for library elements.
     *
     * If you have a use-site module in hand, please pass it as an argument to stay consistent. In the future, we may utilize the use-site
     * module for additional purposes not listed above.
     *
     * #### Outsider Modules
     *
     * Normally, every Kotlin source file either belongs to some module (e.g. a source module, or a library module), or is self-contained
     * (a script file, or a file outside content roots). However, in certain cases there might be special modules that include both
     * existing source files, and also some additional files.
     *
     * An example of such a module is one that owns an 'outsider' source file. Outsiders are used in IntelliJ for displaying files that
     * technically belong to some module, but are not included in the module's content roots (e.g. a file from a previous VCS revision).
     * As there might be cross-references between the outsider file and other files in the module, they need to be analyzed as a single
     * synthetic module. Inside an analysis session for such a module (which would be the [useSiteModule]), sources that originally
     * belong to a source module should be treated rather as a part of the synthetic one.
     */
    public fun getModule(element: PsiElement, useSiteModule: KtModule?): KtModule

    public companion object {
        public fun getInstance(project: Project): KaModuleProvider = project.service()

        public fun getModule(project: Project, element: PsiElement, useSiteModule: KtModule?): KtModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
