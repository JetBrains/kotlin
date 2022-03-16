/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.frontend.fir.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalStdlibApi::class)
abstract class TestKtModule(
    val project: Project,
    val testModule: TestModule,
    val ktFiles: Set<KtFile>,
    testServices: TestServices,
) {
    private val moduleProvider = testServices.projectModuleProvider
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)


    val moduleName: String
        get() = testModule.name

    val directRegularDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.allDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) as KtModule }
            addIfNotNull(
                libraryByRoots(
                    (configuration.jvmModularRoots + configuration.jvmClasspathRoots).map(File::toPath)
                )
            )
        }
    }

    val directRefinementDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        testModule.dependsOnDependencies
            .map { moduleProvider.getModule(it.moduleName) as KtModule }
    }

    val directFriendDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.friendDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) as KtModule }
            addIfNotNull(
                libraryByRoots(configuration[JVMConfigurationKeys.FRIEND_PATHS].orEmpty().map(Paths::get))
            )
        }
    }

    protected abstract val ktModule: KtModule

    private fun libraryByRoots(roots: List<Path>): LibraryByRoots? {
        if (roots.isEmpty()) return null
        return LibraryByRoots(
            roots,
            ktModule,
            project,
        )
    }

    val languageVersionSettings: LanguageVersionSettings
        get() = testModule.languageVersionSettings

    val platform: TargetPlatform
        get() = testModule.targetPlatform

    val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}

class TestKtSourceModule(
    project: Project,
    testModule: TestModule,
    val testFilesToKtFiles: Map<TestFile, KtFile>,
    testServices: TestServices
) : TestKtModule(project, testModule, testFilesToKtFiles.values.toSet(), testServices), KtSourceModule {
    override val ktModule: KtModule get() = this

    override val contentScope: GlobalSearchScope =
        TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, testFilesToKtFiles.values)
}

class TestKtLibraryModule(
    project: Project,
    testModule: TestModule,
    ktFiles: Collection<KtFile>,
    testServices: TestServices
) : TestKtModule(project, testModule, ktFiles.toSet(), testServices), KtLibraryModule {
    override val ktModule: KtModule get() = this
    override val libraryName: String get() = testModule.name
    override val librarySources: KtLibrarySourceModule? get() = null

    override fun getBinaryRoots(): Collection<Path> = ktFiles.map { it.virtualFile.toNioPath() }

    override val contentScope: GlobalSearchScope =
        GlobalSearchScope.filesScope(project, ktFiles.map { it.virtualFile })
}

class TestKtLibrarySourceModule(
    project: Project,
    testModule: TestModule,
    ktFilesFromSourceJar: Set<KtFile>,
    testServices: TestServices,
    override val binaryLibrary: KtLibraryModule,
) : TestKtModule(project, testModule, ktFilesFromSourceJar, testServices), KtLibrarySourceModule {
    override val ktModule: KtModule get() = this
    override val contentScope: GlobalSearchScope get() = GlobalSearchScope.filesScope(project, ktFiles.map { it.virtualFile })

    override val libraryName: String get() = testModule.name
}

private class LibraryByRoots(
    private val roots: List<Path>,
    private val module: KtModule,
    override val project: Project,
) : KtLibraryModule {
    override val libraryName: String get() = "Test Library"
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directRefinementDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()
    override val contentScope: GlobalSearchScope get() = ProjectScope.getLibrariesScope(project)
    override val platform: TargetPlatform get() = module.platform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = module.analyzerServices
    override fun getBinaryRoots(): Collection<Path> = roots
    override val librarySources: KtLibrarySourceModule? get() = null
}

