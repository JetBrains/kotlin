/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory.findJvmRootsForJavaFiles
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure

/**
 * [KtTestModule] describes a [KaModule] originating from a [TestModule] with any number of associated [PsiFile]s. It is used to describe
 * the module structure configured by Analysis API tests.
 */
class KtTestModule(
    val moduleKind: TestModuleKind,
    val testModule: TestModule,
    val ktModule: KaModule,
    files: List<PsiFile>,
) {
    init {
        assert(testModule.files.size == files.size) {
            "The number of provided 'PsiFile's is not the same as the number of 'TestFile's from 'TestModule'"
        }
    }

    val testFiles: List<KtTestFile> = testModule.files.zip(files).map { (testFile, psiFile) ->
        if (psiFile is KtFile) {
            KtTestKtFile(testFile, psiFile)
        } else {
            KtTestFile(testFile, psiFile)
        }
    }

    val ktFiles: List<KtTestKtFile>
        get() = testFiles.filterIsInstance<KtTestKtFile>()
}

/**
 * [KtTestFile] describes a [TestFile] originating from a [KtTestModule] with a corresponding [PsiFile].
 * It is used to conveniently represent files in Analysis API test infrastructure,
 * as sometimes it's necessary to work with [TestFile] and [PsiFile] in parallel.
 */
open class KtTestFile(val testFile: TestFile, open val psiFile: PsiFile)

/**
 * [KtTestKtFile] describes a [KtTestFile] with [psiFile] being an instance of [KtFile].
 */
class KtTestKtFile(testFile: TestFile, override val psiFile: KtFile) : KtTestFile(testFile, psiFile)


/**
 * A module structure of [KtTestModule]s, and additional [KaLibraryModule]s not originating from test modules. This module structure
 * is created by [AnalysisApiTestConfigurator.createModules][org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator.createModules].
 *
 * [mainModules] are created from the configured [TestModule]s and must be in the same order as [testModuleStructure]'s
 * [modules][TestModuleStructure.modules].
 */
class KtTestModuleStructure(
    val testModuleStructure: TestModuleStructure,
    val mainModules: List<KtTestModule>,
    val binaryModules: Iterable<KaLibraryModule>,
) {
    private val mainModulesByName: Map<String, KtTestModule> = mainModules.associateBy { it.testModule.name }

    val project: Project get() = mainModules.first().ktModule.project

    val allMainKtFiles: List<KtFile> get() = mainModules.flatMap { module -> module.ktFiles.map { it.psiFile } }

    val mainAndBinaryKtModules: List<KaModule>
        get() = buildList {
            mainModules.mapTo(this) { it.ktModule }
            addAll(binaryModules)
        }

    val allSourceFiles: List<PsiFileSystemItem>
        get() = buildList {
            val files = mainModules
                .filter { it.ktModule.canContainSourceFiles }
                .flatMap { it.testFiles }
                .map { it.psiFile }

            addAll(files)
            addAll(findJvmRootsForJavaFiles(files.filterIsInstance<PsiJavaFile>()))
        }

    private val KaModule.canContainSourceFiles: Boolean
        get() = when (this) {
            is KaSourceModule, is KaScriptModule, is KaDanglingFileModule, is KaNotUnderContentRootModule -> true
            else -> false
        }

    fun getKtTestModule(moduleName: String): KtTestModule {
        return mainModulesByName.getValue(moduleName)
    }

    fun getKtTestModule(testModule: TestModule): KtTestModule {
        return mainModulesByName[testModule.name] ?: mainModulesByName.getValue(testModule.files.single().name)
    }
}
