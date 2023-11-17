/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addIfNotNull
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

object TestModuleStructureFactory {
    fun createProjectStructureByTestStructure(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        val moduleEntries = moduleStructure.modules
            .map { testModule -> testServices.ktModuleFactory.createModule(testModule, testServices, project) }

        val moduleEntriesByName = moduleEntries.associateByName()

        val binaryModulesBySourceRoots = mutableMapOf<Set<Path>, KtBinaryModule>()

        for (testModule in moduleStructure.modules) {
            val moduleWithFiles = moduleEntriesByName[testModule.name] ?: moduleEntriesByName.getValue(testModule.files.single().name)
            when (val ktModule = moduleWithFiles.ktModule) {
                is KtNotUnderContentRootModule -> {
                    // Not-under-content-root modules have no external dependencies on purpose
                }
                is KtModuleWithModifiableDependencies -> {
                    addModuleDependencies(testModule, moduleEntriesByName, ktModule)
                    stdlibAndSdkDependencies(ktModule, testModule, project, testServices).forEach { library ->
                        val cachedLibrary = binaryModulesBySourceRoots.getOrPut(library.getBinaryRoots().toSet()) { library }
                        ktModule.directRegularDependencies.add(cachedLibrary)
                    }
                }
                else -> error("Unexpected module type: " + ktModule.javaClass.name)
            }
        }

        return KtModuleProjectStructure(moduleEntries, binaryModulesBySourceRoots.values)
    }

    private fun stdlibAndSdkDependencies(
        module: KtModule,
        testModule: TestModule,
        project: Project,
        testServices: TestServices,
    ): List<KtBinaryModule> {
        return when {
            module.platform.isJvm() -> jvmStdlibAndJdkDependencies(testModule, project, testServices)
            module.platform.isJs() -> jsStdlibDependencies(project, testServices)
            else -> error("Unsupported platform ${module.platform} in this test")
        }
    }

    private fun jvmStdlibAndJdkDependencies(testModule: TestModule, project: Project, testServices: TestServices): List<KtBinaryModule> {
        return buildList {
            addIfNotNull(getJdkModule(testModule, project, testServices))
            addAll(getStdlibModules(testModule, project, testServices))
            addAll(getLibraryModules(testServices, testModule, project))
            addAll(createLibrariesByCompilerConfigurators(testModule, testServices, project))
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    private fun createLibrariesByCompilerConfigurators(
        testModule: TestModule,
        testServices: TestServices,
        project: Project
    ): List<KtLibraryModuleImpl> {
        val compilerConfiguration = createCompilerConfiguration(testModule, testServices.environmentConfigurators)
        val contentRoots = compilerConfiguration[CLIConfigurationKeys.CONTENT_ROOTS, emptyList()]
        return contentRoots
            .filterIsInstance<JvmClasspathRoot>()
            .map { root -> createKtLibraryModuleByJar(root.file.toPath(), testServices, project) }
    }

    private fun addModuleDependencies(testModule: TestModule, moduleByName: Map<String, KtModuleWithFiles>, ktModule: KtModule) {
        requireIsInstance<KtModuleWithModifiableDependencies>(ktModule)
        testModule.allDependencies.forEach { dependency ->
            val dependencyKtModule = moduleByName.getValue(dependency.moduleName).ktModule
            when (dependency.relation) {
                DependencyRelation.RegularDependency -> ktModule.directRegularDependencies.add(dependencyKtModule)
                DependencyRelation.FriendDependency -> ktModule.directFriendDependencies.add(dependencyKtModule)
                DependencyRelation.DependsOnDependency -> ktModule.directDependsOnDependencies.add(dependencyKtModule)
            }
        }
    }

    private fun getLibraryModules(
        testServices: TestServices,
        testModule: TestModule,
        project: Project
    ): List<KtLibraryModuleImpl> {
        val configurationKind = JvmEnvironmentConfigurator.extractConfigurationKind(testModule.directives)
        return JvmEnvironmentConfigurator
            .getLibraryFilesExceptRealRuntime(testServices, configurationKind, testModule.directives)
            .map { it.toPath().toAbsolutePath() }
            .map { jar ->
                createKtLibraryModuleByJar(
                    jar,
                    testServices,
                    project,
                )
            }
    }

    private fun createKtLibraryModuleByJar(
        jar: Path,
        testServices: TestServices,
        project: Project,
        libraryName: String = jar.nameWithoutExtension,
    ): KtLibraryModuleImpl {
        check(jar.extension == "jar")
        check(jar.exists()) {
            "library $jar does not exist"
        }
        return KtLibraryModuleImpl(
            libraryName,
            JvmPlatforms.defaultJvmPlatform,
            StandaloneProjectFactory.createSearchScopeByLibraryRoots(
                listOf(jar),
                testServices.environmentManager.getProjectEnvironment()
            ),
            project,
            listOf(jar),
            librarySources = null,
        )
    }

    private fun getStdlibModules(
        testModule: TestModule,
        project: Project,
        testServices: TestServices,
    ): List<KtLibraryModule> {
        val configurationKind = JvmEnvironmentConfigurator.extractConfigurationKind(testModule.directives)
        if (!configurationKind.withRuntime) return emptyList()
        val lib = testServices.standardLibrariesPathProvider.runtimeJarForTests().toPath().absolute()
        return listOf(
            createKtLibraryModuleByJar(lib, testServices, project, PathUtil.KOTLIN_JAVA_STDLIB_NAME),
        )
    }

    private fun getJdkModule(
        testModule: TestModule,
        project: Project,
        testServices: TestServices,
    ): KtJdkModuleImpl? {
        val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(testModule.directives)

        val jdkSourceRoots = buildList {
            JvmEnvironmentConfigurator.getJdkHome(jdkKind)?.let { add(it.toPath()) }
            JvmEnvironmentConfigurator.getJdkClasspathRoot(jdkKind)?.let { add(it.toPath()) }
        }.mapTo(mutableListOf()) { it.toAbsolutePath() }

        if (jdkSourceRoots.isEmpty()) return null

        return KtJdkModuleImpl(
            "jdk",
            JvmPlatforms.defaultJvmPlatform,
            StandaloneProjectFactory.createSearchScopeByLibraryRoots(
                jdkSourceRoots,
                testServices.environmentManager.getProjectEnvironment()
            ),
            project,
            jdkSourceRoots
        )
    }

    private fun jsStdlibDependencies(project: Project, testServices: TestServices): List<KtBinaryModule> {
        return listOf(
            jsStdlib(project, testServices),
        )
    }

    private fun jsStdlib(project: Project, testServices: TestServices): KtBinaryModule {
        val jar = testServices.standardLibrariesPathProvider.fullJsStdlib().toPath()
        return KtLibraryModuleImpl(
            PathUtil.JS_LIB_NAME,
            JvmPlatforms.defaultJvmPlatform,
            StandaloneProjectFactory.createSearchScopeByLibraryRoots(
                listOf(jar),
                testServices.environmentManager.getProjectEnvironment()
            ),
            project,
            listOf(jar),
            librarySources = null,
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

