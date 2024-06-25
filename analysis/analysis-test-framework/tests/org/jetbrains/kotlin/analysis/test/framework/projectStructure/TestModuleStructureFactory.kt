/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
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

private typealias LibraryCache = MutableMap<Set<Path>, KaLibraryModule>

private typealias ModulesByName = Map<String, KtTestModule>

object TestModuleStructureFactory {
    fun createProjectStructureByTestStructure(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtTestModuleStructure {
        val modules = createModules(moduleStructure, testServices, project)

        val modulesByName = modules.associateBy { it.testModule.name }

        val libraryCache: LibraryCache = mutableMapOf()

        for (ktTestModule in modules) {
            ktTestModule.ktModule.addToLibraryCacheIfNeeded(libraryCache)
            ktTestModule.addDependencies(testServices, modulesByName, libraryCache)
        }

        return KtTestModuleStructure(moduleStructure, modules, libraryCache.values)
    }

    /**
     * The test infrastructure ensures that the given [moduleStructure] contains properly ordered dependencies: a [TestModule] can only
     * depend on test modules which precede it. Hence, this function does not need to order dependencies itself.
     *
     * @return A list of [KtTestModule]s in the same order as [TestModuleStructure.modules].
     */
    private fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): List<KtTestModule> {
        val moduleCount = moduleStructure.modules.size
        val existingModules = HashMap<String, KtTestModule>(moduleCount)
        val result = ArrayList<KtTestModule>(moduleCount)

        for (testModule in moduleStructure.modules) {
            val contextModuleName = testModule.directives.singleOrZeroValue(AnalysisApiTestDirectives.CONTEXT_MODULE)
            val contextModule = contextModuleName?.let(existingModules::getValue)

            val dependencyBinaryRoots = testModule.regularDependencies.flatMap { dependency ->
                val libraryModule = existingModules.getValue(dependency.moduleName).ktModule as? KaLibraryModule
                libraryModule?.binaryRoots.orEmpty()
            }

            val ktTestModule = testServices
                .getKtModuleFactoryForTestModule(testModule)
                .createModule(testModule, contextModule, dependencyBinaryRoots, testServices, project)

            existingModules[testModule.name] = ktTestModule
            result.add(ktTestModule)
        }

        return result
    }

    /**
     * A main module may be a binary library module, which may be a dependency of subsequent main modules. We need to add such a module to
     * the library cache before it is processed as a dependency. Otherwise, when another module's binary dependency is processed,
     * [addLibraryDependencies] will create a *duplicate* binary library module with the same roots and name as the already existing binary
     * library module.
     */
    private fun KaModule.addToLibraryCacheIfNeeded(libraryCache: LibraryCache) {
        if (this is KaLibraryModule) {
            libraryCache.put(binaryRoots.toSet(), this)
        }
    }

    private fun KtTestModule.addDependencies(
        testServices: TestServices,
        modulesByName: ModulesByName,
        libraryCache: LibraryCache,
    ) = when (ktModule) {
        is KaNotUnderContentRootModule, is KaBuiltinsModule -> {
            // Not-under-content-root and builtin modules have no external dependencies on purpose
        }
        is KaDanglingFileModule -> {
            // Dangling file modules get dependencies from their context
        }
        is KtModuleWithModifiableDependencies -> {
            addModuleDependencies(testModule, modulesByName, ktModule)
            addLibraryDependencies(testModule, testServices, ktModule, libraryCache::getOrPut)
        }
        else -> error("Unexpected module type: " + ktModule.javaClass.name)
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
        ktModule: KtModuleWithModifiableDependencies,
        libraryCache: (paths: Set<Path>, factory: () -> KaLibraryModule) -> KaLibraryModule
    ) {
        val project = ktModule.project

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
                    KaLibraryModuleImpl(
                        "jdk",
                        JvmPlatforms.defaultJvmPlatform,
                        jdkScope,
                        project,
                        jdkRoots,
                        librarySources = null,
                        isSdk = true,
                    )
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
    ): KaLibraryModuleImpl {
        check(libraryFile.exists()) { "Library $libraryFile does not exist" }

        val libraryName = libraryFile.nameWithoutExtension
        val libraryScope = getScopeForLibraryByRoots(listOf(libraryFile), testServices)
        return KaLibraryModuleImpl(libraryName, platform, libraryScope, project, listOf(libraryFile), librarySources = null, isSdk = false)
    }

    fun getScopeForLibraryByRoots(roots: Collection<Path>, testServices: TestServices): GlobalSearchScope {
        return StandaloneProjectFactory.createSearchScopeByLibraryRoots(
            roots,
            emptyList(),
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
                    val filePath = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile)
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

