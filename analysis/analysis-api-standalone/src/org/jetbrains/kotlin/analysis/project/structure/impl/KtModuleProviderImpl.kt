/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.*

internal class KtModuleProviderImpl(
    private val mainModules: List<KtModule>,
) : ProjectStructureProvider() {
    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        val containingFile = element.containingFile.virtualFile
        return mainModules.first { module ->
            containingFile in module.contentScope
        }
    }

    private val binaryModules: Collection<KtBinaryModule> by lazy {
        mainModules
            .flatMap { it.allDirectDependencies() }
            .filterIsInstance<KtBinaryModule>()
    }

    override fun getKtBinaryModules(): Collection<KtBinaryModule> {
        return binaryModules
    }

    override fun getStdlibWithBuiltinsModule(module: KtModule): KtLibraryModule? {
        return binaryModules
            .filterIsInstance<KtLibraryModuleImpl>()
            .firstOrNull { it.isBuiltinsContainingStdlib }
    }
}