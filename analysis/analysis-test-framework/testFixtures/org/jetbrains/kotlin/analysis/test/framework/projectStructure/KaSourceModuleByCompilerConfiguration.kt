/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.getVirtualFilesForLibraryRoots
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.frontend.fir.getAllJsDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.getAllWasmDependenciesPaths
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

abstract class KtModuleByCompilerConfiguration(
    override val project: Project,
    val testModule: TestModule,
    val psiFiles: List<PsiFile>,
    val testServices: TestServices,
) : KaModuleBase() {
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule, CompilationStage.FIRST)

    val name: String
        get() = testModule.name

    override val directRegularDependencies: List<KaModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.allDependencies.mapTo(this) { testServices.ktTestModuleStructure.getKtTestModule(it.dependencyModule.name).ktModule }
            addAll(computeLibraryDependencies())
        }
    }

    private fun computeLibraryDependencies(): List<KaLibraryModule> {
        val targetPlatform = testModule.targetPlatform(testServices)
        return when {
            targetPlatform.isNative() -> {
                librariesByRoots(getAllNativeDependenciesPaths(testModule, testServices).map { Paths.get(it) })
            }
            targetPlatform.isWasm() -> {
                librariesByRoots(getAllWasmDependenciesPaths(testModule, testServices, configuration.wasmTarget).map { Paths.get(it) })
            }
            targetPlatform.isJs() -> {
                librariesByRoots(getAllJsDependenciesPaths(testModule, testServices).map { Paths.get(it) })
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

    private fun createJdkFromConfiguration(): KaLibraryModule? = configuration.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
        val jdkHomePaths = StandaloneProjectFactory.getDefaultJdkModulePaths(project, jdkHome.toPath())
        val libraryVirtualFiles = getVirtualFilesForLibraryRoots(
            jdkHomePaths,
            testServices.environmentManager.getApplicationEnvironment()
        )

        val scope = StandaloneProjectFactory.createLibraryModuleSearchScope(
            libraryVirtualFiles,
            project
        )

        KaLibraryModuleImpl(
            "jdk",
            JvmPlatforms.defaultJvmPlatform,
            scope,
            project,
            jdkHomePaths,
            libraryVirtualFiles,
            librarySources = null,
            isSdk = true,
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    override // used for overrides in subclasses
    val directDependsOnDependencies: List<KaModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        testModule.dependsOnDependencies
            .map { testServices.ktTestModuleStructure.getKtTestModule(it.dependencyModule.name).ktModule }
    }

    override val directFriendDependencies: List<KaModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.friendDependencies.mapTo(this) { testServices.ktTestModuleStructure.getKtTestModule(it.dependencyModule.name).ktModule }
            addAll(
                librariesByRoots(configuration[JVMConfigurationKeys.FRIEND_PATHS].orEmpty().map(Paths::get))
            )
        }
    }

    protected abstract val ktModule: KaModule

    private fun librariesByRoots(roots: List<Path>): List<LibraryByRoots> = roots.map { LibraryByRoots(listOf(it), ktModule, project, testServices) }

    val languageVersionSettings: LanguageVersionSettings
        get() = testModule.languageVersionSettings

    override val targetPlatform: TargetPlatform
        get() = testModule.targetPlatform(testServices)
}

class KaSourceModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    psiFiles: List<PsiFile>,
    testServices: TestServices,
    override val baseContentScope: GlobalSearchScope
) : KtModuleByCompilerConfiguration(project, testModule, psiFiles, testServices), KaSourceModule {
    override val ktModule: KaModule get() = this

    @KaExperimentalApi
    override val psiRoots: List<PsiFileSystemItem>
        get() = psiFiles
}

class KaScriptModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    override val file: KtFile,
    testServices: TestServices,
) : KtModuleByCompilerConfiguration(project, testModule, listOf(file), testServices), KaScriptModule {
    override val ktModule: KaModule get() = this
    override val baseContentScope: GlobalSearchScope get() = GlobalSearchScope.fileScope(file)
}

class KaLibraryModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    psiFiles: List<PsiFile>,
    @Deprecated("Use `binaryVirtualFiles` instead. See KT-72676", replaceWith = ReplaceWith("binaryVirtualFiles"))
    override val binaryRoots: List<Path>,
    testServices: TestServices
) : KtModuleByCompilerConfiguration(project, testModule, psiFiles, testServices), KaLibraryModule {
    override val ktModule: KaModule get() = this
    override val libraryName: String get() = testModule.name
    override val librarySources: KaLibrarySourceModule? get() = null
    override val isSdk: Boolean get() = false

    @Suppress("DEPRECATION")
    override val binaryVirtualFiles: Collection<VirtualFile> =
        getVirtualFilesForLibraryRoots(binaryRoots, testServices.environmentManager.getApplicationEnvironment())

    override val baseContentScope: GlobalSearchScope =
        GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })
}

class KaLibrarySourceModuleByCompilerConfiguration(
    project: Project,
    testModule: TestModule,
    psiFiles: List<PsiFile>,
    testServices: TestServices,
    override val binaryLibrary: KaLibraryModule,
) : KtModuleByCompilerConfiguration(project, testModule, psiFiles, testServices), KaLibrarySourceModule {
    override val ktModule: KaModule get() = this
    override val baseContentScope: GlobalSearchScope get() = GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })

    override val libraryName: String get() = testModule.name
}

private class LibraryByRoots(
    private val roots: List<Path>,
    private val parentModule: KaModule,
    override val project: Project,
    testServices: TestServices,
) : KaLibraryModule, KaModuleBase() {
    override val libraryName: String get() = "Test Library $roots"
    override val directRegularDependencies: List<KaModule> get() = emptyList()
    override val directDependsOnDependencies: List<KaModule> get() = emptyList()
    override val transitiveDependsOnDependencies: List<KaModule> get() = emptyList()
    override val directFriendDependencies: List<KaModule> get() = emptyList()
    override val targetPlatform: TargetPlatform get() = parentModule.targetPlatform

    @Deprecated("Use `binaryVirtualFiles` instead. See KT-72676", replaceWith = ReplaceWith("binaryVirtualFiles"))
    override val binaryRoots: Collection<Path> get() = roots

    override val isSdk: Boolean get() = false
    override val binaryVirtualFiles: Collection<VirtualFile> =
        getVirtualFilesForLibraryRoots(roots, testServices.environmentManager.getApplicationEnvironment())

    override val baseContentScope: GlobalSearchScope = StandaloneProjectFactory.createLibraryModuleSearchScope(
        binaryVirtualFiles,
        project,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LibraryByRoots

        return roots == other.roots
    }

    override fun hashCode(): Int {
        return roots.hashCode()
    }

    override val librarySources: KaLibrarySourceModule? get() = null
}
