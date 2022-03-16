/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestKtLibraryModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.projectModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class LibraryModuleRegistrarPreAnalysisHandler(
    testServices: TestServices
) : PreAnalysisHandler(testServices) {
    private val moduleInfoProvider = testServices.projectModuleProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        val testModule = moduleStructure.modules.single()
        val project = testServices.compilerConfigurationProvider.getProject(testModule)
        val decompiledKtFiles = getDecompiledVirtualFilesFromLibrary(testModule, project)

        moduleInfoProvider.registerModuleInfo(testModule, TestKtLibraryModule(project, testModule, decompiledKtFiles, testServices))
    }

    private fun getDecompiledVirtualFilesFromLibrary(module: TestModule, project: Project): List<KtClsFile> {
        val library = testServices.compiledLibraryProvider.getCompiledLibrary(module).jar

        val virtualFiles = LibraryUtils.getAllVirtualFilesFromJar(library)
        return virtualFiles.mapNotNull { virtualFile ->
            PsiManager.getInstance(project).findFile(virtualFile) as? KtClsFile
        }
    }
}
