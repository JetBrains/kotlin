/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.KtTestUtil

fun getKtFilesFromModule(testServices: TestServices, testModule: TestModule): List<KtFile> {
    val moduleInfoProvider = testServices.ktModuleProvider
    return when (val moduleInfo = moduleInfoProvider.getModule(testModule.name)) {
        is TestKtSourceModule -> moduleInfo.psiFiles.filterIsInstance<KtFile>()
        is TestKtLibraryModule -> moduleInfo.psiFiles.filterIsInstance<KtFile>()
        is TestKtLibrarySourceModule -> moduleInfo.psiFiles.filterIsInstance<KtFile>()
        else -> error("Unexpected $moduleInfo")
    }
}


fun TestModuleStructure.toKtSourceModules(testServices: TestServices, project: Project) =
    modules.map { testModule -> testModule.toKtSourceModule(testServices, project) }

fun TestModule.toKtSourceModule(testServices: TestServices, project: Project): KtModuleWithFiles {
    val files = files.map { testFile ->
        when {
            testFile.isKtFile -> {
                val fileText = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                KtTestUtil.createFile(testFile.name, fileText, project)
            }

            testFile.isJavaFile -> {
                val filePath = testServices.sourceFileProvider.getRealFileForSourceFile(testFile)
                val virtualFile =
                    testServices.environmentManager.getApplicationEnvironment().localFileSystem.findFileByIoFile(filePath)
                        ?: error("Virtual file not found for $filePath")
                PsiManager.getInstance(project).findFile(virtualFile)
                    ?: error("PsiFile file not found for $filePath")
            }

            else -> error("Unexpected file ${testFile.name}")
        }
    }
    return KtModuleWithFiles(
        TestKtSourceModule(project, this, files, testServices),
        files
    )
}