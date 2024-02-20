/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory.findJvmRootsForJavaFiles
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import kotlin.collections.addAll
import kotlin.collections.filterIsInstance
import kotlin.collections.flatMap
import kotlin.collections.mapTo

/**
 * [KtTestModule] describes a [KtModule] originating from a [TestModule] with any number of associated [PsiFile]s. It is used to describe
 * the module structure configured by Analysis API tests.
 */
class KtTestModule(
    val moduleKind: TestModuleKind,
    val testModule: TestModule,
    val ktModule: KtModule,
    val files: List<PsiFile>,
)

/**
 * A project structure of [KtTestModule]s, and additional [KtBinaryModule]s not originating from test modules. This project structure
 * is created by [AnalysisApiTestConfigurator.createModules][org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator.createModules].
 *
 * [mainModules] are created from the configured [TestModule]s and must be in the same order as
 * [TestModuleStructure.modules][org.jetbrains.kotlin.test.services.TestModuleStructure.modules].
 */
class KtTestModuleProjectStructure(
    val mainModules: List<KtTestModule>,
    val binaryModules: Iterable<KtBinaryModule>,
) {
    fun allKtModules(): List<KtModule> = buildList {
        mainModules.mapTo(this) { it.ktModule }
        addAll(binaryModules)
    }

    fun allSourceFiles(): List<PsiFileSystemItem> = buildList {
        val files = mainModules.flatMap { it.files }
        addAll(files)
        addAll(findJvmRootsForJavaFiles(files.filterIsInstance<PsiJavaFile>()))
    }
}
