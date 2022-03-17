/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

interface AnalysisApiTestConfiguratorService {
    val testPrefix: String? get() = null

    val analyseInDependentSession: Boolean

    fun TestConfigurationBuilder.configureTest(disposable: Disposable)
    fun processTestFiles(files: List<KtFile>): List<KtFile> = files

    fun getOriginalFile(file: KtFile): KtFile = file
    fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider
    )

    fun registerApplicationServices(application: MockApplication)

    fun prepareTestFiles(files: List<KtFile>, module: TestModule, testServices: TestServices) {}

    fun doOutOfBlockModification(file: KtFile)

    fun preprocessTestDataPath(path: Path): Path = path
}
