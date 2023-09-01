/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider

class LLModuleProvider(val useSiteModule: KtModule) {
    fun getModule(element: PsiElement): KtModule {
        return ProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
    }
}