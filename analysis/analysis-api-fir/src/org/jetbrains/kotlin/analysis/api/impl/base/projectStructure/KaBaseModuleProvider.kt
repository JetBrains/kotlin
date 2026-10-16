/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider

@KaImplementationDetail
class KaBaseModuleProvider(private val project: Project) : KaModuleProvider {
    override fun module(element: PsiElement, useSiteModule: KaModule?): KaModule {
        return KotlinProjectStructureProvider.getModule(project, element, useSiteModule)
    }

    @Deprecated("Use 'module' instead", replaceWith = ReplaceWith("module(element, useSiteModule)"))
    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule = module(element, useSiteModule)
}
