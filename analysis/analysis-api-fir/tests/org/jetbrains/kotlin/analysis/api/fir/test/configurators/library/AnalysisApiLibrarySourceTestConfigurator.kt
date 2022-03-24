/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.configurators.library

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.test.configurators.source.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiLibraryBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AnalysisApiFirTestServiceRegistrar
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestKtLibrarySourceModule
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.fir.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

object AnalysisApiLibrarySourceTestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false

    override fun configureTest(
        builder: TestConfigurationBuilder,
        disposable: Disposable
    ) {
        builder.useAdditionalServices(ServiceRegistrationData(CompiledLibraryProvider::class, ::CompiledLibraryProvider))
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): List<KtModuleWithFiles> {
        val testModule = moduleStructure.modules.single()
        val (libraryJar, librarySourcesJar) = testServices.compiledLibraryProvider.compileToLibrary(testModule)

        val libraryKtModule = KtTestLibraryModule(project, testModule, libraryJar)

        val librarySourceFiles = LibraryUtils
            .getAllPsiFilesFromTheJar(librarySourcesJar, project)

        val sourcesKtModule = TestKtLibrarySourceModule(
            project,
            testModule,
            librarySourceFiles,
            testServices,
            libraryKtModule
        )
        libraryKtModule.librarySources = sourcesKtModule

        return listOf(
            KtModuleWithFiles(sourcesKtModule, librarySourceFiles),
        )
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

private class KtTestLibraryModule(
    override val project: Project,
    private val testModule: TestModule,
    private val jar: Path
) : KtLibraryModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directRefinementDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()

    override val contentScope: GlobalSearchScope by lazy {
        GlobalSearchScope.filesScope(project, LibraryUtils.getAllVirtualFilesFromJar(jar))
    }

    override fun getBinaryRoots(): Collection<Path> = listOf(jar)
    override val libraryName: String get() = testModule.name

    override lateinit var librarySources: KtLibrarySourceModule

    override val platform: TargetPlatform get() = testModule.targetPlatform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = testModule.targetPlatform.getAnalyzerServices()
}

