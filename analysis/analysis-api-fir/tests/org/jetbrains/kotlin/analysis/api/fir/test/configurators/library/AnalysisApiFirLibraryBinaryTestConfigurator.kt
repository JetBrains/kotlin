/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.configurators.library

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiLibraryBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AnalysisApiFirTestServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtLibraryModuleImpl
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFirLibraryBinaryTestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false
    override val frontendKind: FrontendKind get() = FrontendKind.Fir

    override fun configureTest(
        builder: TestConfigurationBuilder,
        disposable: Disposable
    ) {
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        return TestModuleStructureFactory.createProjectStructureByTestStructure(
            moduleStructure,
            testServices,
            project
        ) { testModule: TestModule, _, _ ->
            val library = testServices.compiledLibraryProvider.compileToLibrary(testModule).jar
            val decompiledFiles = LibraryUtils.getAllPsiFilesFromTheJar(library, project)

            KtModuleWithFiles(
                KtLibraryModuleImpl(
                    testModule.name,
                    testModule.targetPlatform,
                    GlobalSearchScope.filesScope(project, decompiledFiles.mapTo(mutableSetOf()) { it.virtualFile }),
                    project,
                    binaryRoots = listOf(library),
                    librarySources = null,
                ),
                decompiledFiles
            )
        }
    }


    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> =
        listOf(
            AnalysisApiBaseTestServiceRegistrar,
            AnalysisApiFirTestServiceRegistrar,
            AnalysisApiLibraryBaseTestServiceRegistrar,
        )


    override fun doOutOfBlockModification(file: KtFile) {
        AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false).doOutOfBlockModification(file)
    }
}
