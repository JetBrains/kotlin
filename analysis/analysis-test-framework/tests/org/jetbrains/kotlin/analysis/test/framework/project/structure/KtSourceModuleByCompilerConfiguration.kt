/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

abstract class KtModuleByCompilerConfiguration(
    val project: Project,
    val testModule: TestModule,
    val psiFiles: List<PsiFile>,
    val testServices: TestServices,
) {
    private val moduleProvider = testServices.ktModuleProvider
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)


    val moduleName: String
        get() = testModule.name

    val directRegularDependencies: List<KtModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.allDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addAll(computeLibraryDependencies())
        }
    }

    private fun computeLibraryDependencies(): List<KtBinaryModule> {
        val targetPlatform = testModule.targetPlatform
        return when {
            targetPlatform.isNative() -> {
                librariesByRoots(getAllNativeDependenciesPaths(testModule, testServices).map { Paths.get(it) })
            }
            else -> buildList {
                val roots = buildList {
                    addAll(configuration.jvmModularRoots.map(File::toPath))
                    addAll(configuration.jvmClasspathRoots.map(File::toPath))
                }
                addAll(librariesByRoots(roots))
                addIfNotNull(createJdkFromConfiguration())
            }
        }
    }

    private fun createJdkFromConfiguration(): KtSdkModule? = configuration.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
        val jdkHomePaths = StandaloneProjectFactory.getDefaultJdkModulePaths(project, jdkHome.toPath())
        val scope = StandaloneProjectFactory.createSearchScopeByLibraryRoots(
            jdkHomePaths,
            testServices.environmentManager.getProjectEnvironment()
        )

        KtJdkModuleImpl(
            "jdk",
            JvmPlatforms.defaultJvmPlatform,
            scope,
            project,
            jdkHomePaths,
        )
    }

    @Suppress("MemberVisibilityCanBePrivate") // used for overrides in subclasses
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

    private fun librariesByRoots(roots: List<Path>): List<LibraryByRoots> = roots.map { LibraryByRoots(listOf(it), ktModule, project, testServices) }

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
        GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })
}

class KtScriptModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    override val file: KtFile,
    testServices: TestServices,
) : KtModuleByCompilerConfiguration(project, testModule, listOf(file), testServices), KtScriptModule {
    override val ktModule: KtModule get() = this
    override val contentScope: GlobalSearchScope get() = GlobalSearchScope.fileScope(file)
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

private class LibraryByRoots(
    private val roots: List<Path>,
    private val parentModule: KtModule,
    override val project: Project,
    testServices: TestServices,
) : KtLibraryModule {
    override val contentScope: GlobalSearchScope = StandaloneProjectFactory.createSearchScopeByLibraryRoots(
        roots,
        testServices.environmentManager.getProjectEnvironment(),
    )
    override val libraryName: String get() = "Test Library $roots"
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directDependsOnDependencies: List<KtModule> get() = emptyList()
    override val transitiveDependsOnDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()
    override val platform: TargetPlatform get() = parentModule.platform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = parentModule.analyzerServices
    override fun getBinaryRoots(): Collection<Path> = roots

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LibraryByRoots

        return roots == other.roots
    }

    override fun hashCode(): Int {
        return roots.hashCode()
    }

    override val librarySources: KtLibrarySourceModule? get() = null
}
