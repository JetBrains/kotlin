/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

private typealias LibraryCache = MutableMap<Set<Path>, KtBinaryModule>

private typealias ModulesByName = Map<String, KtModuleWithFiles>

object TestModuleStructureFactory {
    fun createProjectStructureByTestStructure(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        val modules = createModules(moduleStructure, testServices, project)

        val modulesByName = modules.associateByName()

        val libraryCache: LibraryCache = mutableMapOf()

        for (testModule in moduleStructure.modules) {
            val ktModule = modulesByName.getByTestModule(testModule).ktModule

            ktModule.addToLibraryCacheIfNeeded(libraryCache)
            ktModule.addDependencies(testModule, testServices, modulesByName, libraryCache)
        }

        return KtModuleProjectStructure(modules, libraryCache.values)
    }

    private fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): List<KtModuleWithFiles> {
        val moduleCount = moduleStructure.modules.size
        val existingModules = HashMap<String, KtModuleWithFiles>(moduleCount)
        val result = ArrayList<KtModuleWithFiles>(moduleCount)

        for (testModule in moduleStructure.modules) {
            val contextModuleName = testModule.directives.singleOrZeroValue(AnalysisApiTestDirectives.CONTEXT_MODULE)
            val contextModule = contextModuleName?.let(existingModules::getValue)

            val moduleWithFiles = testServices
                .getKtModuleFactoryForTestModule(testModule)
                .createModule(testModule, contextModule, testServices, project)

            existingModules[testModule.name] = moduleWithFiles
            result.add(moduleWithFiles)
        }

        return result
    }

    private fun ModulesByName.getByTestModule(testModule: TestModule): KtModuleWithFiles =
        this[testModule.name] ?: this.getValue(testModule.files.single().name)

    /**
     * A main module may be a binary library module, which may be a dependency of subsequent main modules. We need to add such a module to
     * the library cache before it is processed as a dependency. Otherwise, when another module's binary dependency is processed,
     * [addLibraryDependencies] will create a *duplicate* binary library module with the same roots and name as the already existing binary
     * library module.
     */
    private fun KtModule.addToLibraryCacheIfNeeded(libraryCache: LibraryCache) {
        if (this is KtBinaryModule) {
            libraryCache.put(getBinaryRoots().toSet(), this)
        }
    }

    private fun KtModule.addDependencies(
        testModule: TestModule,
        testServices: TestServices,
        modulesByName: ModulesByName,
        libraryCache: LibraryCache,
    ) = when (this) {
        is KtNotUnderContentRootModule -> {
            // Not-under-content-root modules have no external dependencies on purpose
        }
        is KtDanglingFileModule -> {
            // Dangling file modules get dependencies from their context
        }
        is KtModuleWithModifiableDependencies -> {
            addModuleDependencies(testModule, modulesByName, this)
            addLibraryDependencies(testModule, testServices, project, this, libraryCache::getOrPut)
        }
        else -> error("Unexpected module type: " + javaClass.name)
    }

    private fun addModuleDependencies(
        testModule: TestModule,
        modulesByName: ModulesByName,
        ktModule: KtModuleWithModifiableDependencies,
    ) {
        testModule.allDependencies.forEach { dependency ->
            val dependencyKtModule = modulesByName.getValue(dependency.moduleName).ktModule
            when (dependency.relation) {
                DependencyRelation.RegularDependency -> ktModule.directRegularDependencies.add(dependencyKtModule)
                DependencyRelation.FriendDependency -> ktModule.directFriendDependencies.add(dependencyKtModule)
                DependencyRelation.DependsOnDependency -> ktModule.directDependsOnDependencies.add(dependencyKtModule)
            }
        }
    }

    private fun addLibraryDependencies(
        testModule: TestModule,
        testServices: TestServices,
        project: Project,
        ktModule: KtModuleWithModifiableDependencies,
        libraryCache: (paths: Set<Path>, factory: () -> KtBinaryModule) -> KtBinaryModule
    ) {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(testModule)

        val classpathRoots = compilerConfiguration[CLIConfigurationKeys.CONTENT_ROOTS, emptyList()]
            .mapNotNull { (it as? JvmClasspathRoot)?.file?.toPath() }

        if (classpathRoots.isNotEmpty()) {
            val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(testModule.directives)
            val jdkHome = JvmEnvironmentConfigurator.getJdkHome(jdkKind)?.toPath()
                ?: JvmEnvironmentConfigurator.getJdkClasspathRoot(jdkKind)?.toPath()
                ?: Paths.get(System.getProperty("java.home"))

            val (jdkRoots, libraryRoots) = classpathRoots.partition { jdkHome != null && it.startsWith(jdkHome) }

            if (testModule.targetPlatform.isJvm() && jdkRoots.isNotEmpty()) {
                val jdkModule = libraryCache(jdkRoots.toSet()) {
                    val jdkScope = getScopeForLibraryByRoots(jdkRoots, testServices)
                    KtJdkModuleImpl("jdk", JvmPlatforms.defaultJvmPlatform, jdkScope, project, jdkRoots)
                }
                ktModule.directRegularDependencies.add(jdkModule)
            }

            for (libraryRoot in libraryRoots) {
                check(libraryRoot.extension == "jar")

                val libraryModule = libraryCache(setOf(libraryRoot)) {
                    createLibraryModule(project, libraryRoot, JvmPlatforms.defaultJvmPlatform, testServices)
                }

                ktModule.directRegularDependencies.add(libraryModule)
            }
        }

        val jsLibraryRootPaths = compilerConfiguration[JSConfigurationKeys.LIBRARIES].orEmpty()

        for (libraryRootPath in jsLibraryRootPaths) {
            val libraryRoot = Paths.get(libraryRootPath)
            check(libraryRoot.extension == KLIB_FILE_EXTENSION)

            val libraryModule = libraryCache(setOf(libraryRoot)) {
                createLibraryModule(project, libraryRoot, JsPlatforms.defaultJsPlatform, testServices)
            }

            ktModule.directRegularDependencies.add(libraryModule)
        }
    }

    private fun createLibraryModule(
        project: Project,
        libraryFile: Path,
        platform: TargetPlatform,
        testServices: TestServices,
    ): KtLibraryModuleImpl {
        check(libraryFile.exists()) { "Library $libraryFile does not exist" }

        val libraryName = libraryFile.nameWithoutExtension
        val libraryScope = getScopeForLibraryByRoots(listOf(libraryFile), testServices)
        return KtLibraryModuleImpl(libraryName, platform, libraryScope, project, listOf(libraryFile), librarySources = null)
    }

    fun getScopeForLibraryByRoots(roots: Collection<Path>, testServices: TestServices): GlobalSearchScope {
        return StandaloneProjectFactory.createSearchScopeByLibraryRoots(
            roots,
            testServices.environmentManager.getProjectEnvironment()
        )
    }

    fun createSourcePsiFiles(
        testModule: TestModule,
        testServices: TestServices,
        project: Project,
    ): List<PsiFile> {
        return testModule.files.map { testFile ->
            when {
                testFile.isKtFile -> {
                    val fileText = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                    KtTestUtil.createFile(testFile.name, fileText, project)
                }

                testFile.isJavaFile || testFile.isExternalAnnotation -> {
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
    }
}

