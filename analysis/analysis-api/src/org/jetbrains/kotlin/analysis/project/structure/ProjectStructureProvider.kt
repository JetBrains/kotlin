/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider

@Deprecated(
    "Use 'org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider' instead.",
    ReplaceWith("KaModuleProvider", imports = ["org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider"]),
)
public abstract class ProjectStructureProvider {
    public abstract fun getModule(
        element: PsiElement,
        contextualModule: KtModule?,
    ): KtModule

    public companion object {
        @Deprecated(
            "Use 'org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider.getInstance' instead.",
            ReplaceWith(
                "KaModuleProvider.getInstance(project)",
                imports = ["org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider"],
            ),
        )
        public fun getInstance(project: Project): ProjectStructureProvider =
            object : ProjectStructureProvider() {
                override fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule =
                    KaModuleProvider.getInstance(project).getModule(element, contextualModule)
            }

        @Deprecated(
            "Use 'org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider.getModule' instead.",
            ReplaceWith(
                "KaModuleProvider.getModule(project, element, contextualModule)",
                imports = ["org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider"],
            ),
        )
        public fun getModule(project: Project, element: PsiElement, contextualModule: KtModule?): KtModule =
            getInstance(project).getModule(element, contextualModule)
    }
}
