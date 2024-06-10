/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider

class LLModuleProvider(val useSiteModule: KaModule) {
    /**
     * Returns a [KaModule] for a given [element] in context of the current session.
     *
     * See [KotlinProjectStructureProvider] for more information on contextual modules.
     */
    fun getModule(element: PsiElement): KaModule {
        return KotlinProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
    }
}