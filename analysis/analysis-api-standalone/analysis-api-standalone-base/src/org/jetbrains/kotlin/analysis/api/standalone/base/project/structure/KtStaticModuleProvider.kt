/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.psi.psiUtil.contains

class KtStaticModuleProvider(
    private val modules: List<KtModule>,
    private val stdlibs: Set<KtModule>
) : ProjectStructureProvider() {
    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        return modules.first { module ->
            module.contentScope.contains(element)
        }
    }

    override fun getKtLibraryModules(): Collection<KtLibraryModule> {
        return modules.filterIsInstance<KtLibraryModule>()
    }

    override fun getStdlibModule(module: KtModule): KtLibraryModule? {
        return module.allDirectDependenciesOfType<KtLibraryModule>().firstOrNull { it in stdlibs }
    }
}
