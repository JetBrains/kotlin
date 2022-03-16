/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

class KotlinProjectStructureProviderTestImpl(private val testServices: TestServices) : ProjectStructureProvider() {
    private val moduleInfoProvider = testServices.projectModuleProvider
    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        val containingFile = element.containingFile as KtFile
        return moduleInfoProvider.getModuleInfoByKtFile(containingFile) as KtModule
    }

    override fun getKtLibraryModules(): Collection<TestKtLibraryModule> {
        return moduleInfoProvider.getLibraryModules()
    }
}
