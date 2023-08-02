/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

abstract class KtModuleByCompilerConfiguration(
    val project: Project,
    val testModule: TestModule,
    val psiFiles: List<PsiFile>,
    testServices: TestServices,
) {
    private val moduleProvider = testServices.ktModuleProvider
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)


    val moduleName: String
        get() = testModule.name

    val directRegularDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.allDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addAll(
                librariesByRoots(
                    (configuration.jvmModularRoots + configuration.jvmClasspathRoots).map(File::toPath)
                )
            )
        }
    }

    val directDependsOnDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        testModule.dependsOnDependencies
            .map { moduleProvider.getModule(it.moduleName) }
    }

    val transitiveDependsOnDependencies: List<KtModule> by lazy { computeTransitiveDependsOnDependencies(directDependsOnDependencies) }

    val directFriendDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.friendDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addAll(
                librariesByRoots(configuration[JVMConfigurationKeys.FRIEND_PATHS].orEmpty().map(Paths::get))
            )
        }
    }

    protected abstract val ktModule: KtModule

    private fun librariesByRoots(roots: List<Path>): List<LibraryByRoot> = roots.map { LibraryByRoot(it, ktModule, project) }

    val languageVersionSettings: LanguageVersionSettings
        get() = testModule.languageVersionSettings

    val platform: TargetPlatform
        get() = testModule.targetPlatform

    val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}

class KtSourceModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    psiFiles: List<PsiFile>,
    testServices: TestServices
) : KtModuleByCompilerConfiguration(project, testModule, psiFiles, testServices), KtSourceModule {
    override val ktModule: KtModule get() = this

    override val contentScope: GlobalSearchScope =
        TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, psiFiles.filterIsInstance<KtFile>())
}

class KtLibraryModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    psiFiles: List<PsiFile>,
    private val binaryRoots: List<Path>,
    testServices: TestServices
) : KtModuleByCompilerConfiguration(project, testModule, psiFiles, testServices), KtLibraryModule {
    override val ktModule: KtModule get() = this
    override val libraryName: String get() = testModule.name
    override val librarySources: KtLibrarySourceModule? get() = null

    override fun getBinaryRoots(): Collection<Path> = binaryRoots

    override val contentScope: GlobalSearchScope =
        GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })
}

class KtLibrarySourceModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    psiFiles: List<PsiFile>,
    testServices: TestServices,
    override val binaryLibrary: KtLibraryModule,
) : KtModuleByCompilerConfiguration(project, testModule, psiFiles, testServices), KtLibrarySourceModule {
    override val ktModule: KtModule get() = this
    override val contentScope: GlobalSearchScope get() = GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })

    override val libraryName: String get() = testModule.name
}

private class LibraryByRoot(
    private val root: Path,
    private val parentModule: KtModule,
    override val project: Project,
) : KtLibraryModule {
    override val libraryName: String get() = "Test Library $root"
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directDependsOnDependencies: List<KtModule> get() = emptyList()
    override val transitiveDependsOnDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()
    override val contentScope: GlobalSearchScope get() = ProjectScope.getLibrariesScope(project)
    override val platform: TargetPlatform get() = parentModule.platform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = parentModule.analyzerServices
    override fun getBinaryRoots(): Collection<Path> = listOf(root)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LibraryByRoot

        return root == other.root
    }

    override fun hashCode(): Int {
        return root.hashCode()
    }

    override val librarySources: KtLibrarySourceModule? get() = null
}
