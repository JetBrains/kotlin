/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependenciesOfType
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.utils.errors.checkIsInstance
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addIfNotNull
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

fun interface KtMainModuleFactory {
    fun createMainModule(
        testModule: TestModule,
        testServices: TestServices,
        project: Project,
    ): KtModuleWithFiles
}

object KtMainModuleFactoryForSourceModules : KtMainModuleFactory {
    override fun createMainModule(
        testModule: TestModule,
        testServices: TestServices,
        project: Project,
    ): KtModuleWithFiles {
        val psiFiles = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project)
        return KtModuleWithFiles(
            KtSourceModuleImpl(
                testModule.name,
                testModule.targetPlatform,
                testModule.languageVersionSettings,
                project,
                GlobalSearchScope.filesScope(project, psiFiles.mapTo(mutableSetOf()) { it.virtualFile }),
            ),
            psiFiles
        )
    }
}

object TestModuleStructureFactory {
    @OptIn(TestInfrastructureInternals::class)
    fun createProjectStructureByTestStructure(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
        mainModuleFactory: KtMainModuleFactory
    ): KtModuleProjectStructure {
        val modulesFromTestServices = moduleStructure.modules.map { testModule ->
            testModule.toKtModule(testServices, project, mainModuleFactory)
        }
        val moduleByName = modulesFromTestServices.associateBy { getModuleName(it.ktModule) }
        val binaryModulesBySourceRoots = mutableMapOf<Set<Path>, KtBinaryModule>()

        moduleStructure.modules.forEach { testModule ->
            val ktModule = moduleByName.getValue(testModule.name).ktModule
            checkIsInstance<KtModuleWithModifiableDependencies>(ktModule)

            addModuleDependencies(testModule, moduleByName, ktModule)

            buildList {
                addIfNotNull(getJdkModule(testModule, project))
                addAll(getStdlibModules(testModule, project))
                addAll(getLibraryModules(testServices, testModule, project))
                addAll(createLibrariesByCompilerConfigurators(testModule, testServices, project))
            }.forEach { library ->
                val cachedLibrary = binaryModulesBySourceRoots.getOrPut(library.getBinaryRoots().toSet()) { library }
                ktModule.directRegularDependencies.add(cachedLibrary)
            }

        }
        return KtModuleProjectStructure(modulesFromTestServices, binaryModulesBySourceRoots.values) { module ->
            module.allDirectDependenciesOfType<KtLibraryModuleImpl>().first { it.isBuitinsContainingStdlib }
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
            .map { root -> createKtLibraryModuleByJar(root.file.toPath(), project, isBuitinsContainingStdlib = false) }
    }


    private fun getModuleName(ktModule: KtModule) = when (ktModule) {
        is KtSourceModuleImpl -> ktModule.moduleName
        is KtLibraryModuleImpl -> ktModule.libraryName
        is KtLibrarySourceModuleImpl -> ktModule.libraryName
        else -> error("Unknown module ${ktModule::class}")
    }

    private fun addModuleDependencies(
        testModule: TestModule,
        moduleByName: Map<String, KtModuleWithFiles>,
        ktModule: KtModule
    ) {
        requireIsInstance<KtModuleWithModifiableDependencies>(ktModule)
        testModule.allDependencies.forEach { dependency ->
            val dependencyKtModule = moduleByName.getValue(dependency.moduleName).ktModule
            when (dependency.relation) {
                DependencyRelation.RegularDependency -> ktModule.directRegularDependencies.add(dependencyKtModule)
                DependencyRelation.FriendDependency -> ktModule.directFriendDependencies.add(dependencyKtModule)
                DependencyRelation.DependsOnDependency -> ktModule.directRefinementDependencies.add(dependencyKtModule)
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
            .getLibraryFilesExceptRealRuntime( testServices, configurationKind, testModule.directives)
            .map { it.toPath().toAbsolutePath() }
            .map { jar ->
                createKtLibraryModuleByJar(
                    jar,
                    project,
                    isBuitinsContainingStdlib = jar.nameWithoutExtension == "kotlin-stdlib-jvm-minimal-for-test"
                )
            }
    }

    private fun createKtLibraryModuleByJar(
        jar: Path,
        project: Project,
        libraryName: String = jar.nameWithoutExtension,
        isBuitinsContainingStdlib: Boolean = false,
    ): KtLibraryModuleImpl {
        check(jar.extension == "jar")
        check(jar.exists())
        return KtLibraryModuleImpl(
            libraryName,
            JvmPlatforms.defaultJvmPlatform,
            ProjectScope.getLibrariesScope(project),
            project,
            listOf(jar),
            librarySources = null,
            isBuitinsContainingStdlib,
        )
    }

    private fun getStdlibModules(
        testModule: TestModule,
        project: Project
    ): List<KtLibraryModule> {
        val configurationKind = JvmEnvironmentConfigurator.extractConfigurationKind(testModule.directives)
        if (!configurationKind.withRuntime) return emptyList()
        return listOf(
            KotlinPaths.Jar.StdLib to PathUtil.KOTLIN_JAVA_STDLIB_NAME,
        ).map { (jar, name) ->
            val lib = PathUtil.kotlinPathsForDistDirectory.jar(jar).toPath().toAbsolutePath()
            createKtLibraryModuleByJar(lib, project, name, isBuitinsContainingStdlib = jar == KotlinPaths.Jar.StdLib)
        }
    }

    private fun getJdkModule(
        testModule: TestModule,
        project: Project
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
            ProjectScope.getLibrariesScope(project),
            project,
            jdkSourceRoots
        )
    }

    private fun TestModule.toKtModule(
        testServices: TestServices,
        project: Project,
        mainModuleFactory: KtMainModuleFactory,
    ): KtModuleWithFiles {
        return mainModuleFactory.createMainModule(this, testServices, project)
    }

    fun createSourcePsiFiles(
        testModule: TestModule,
        testServices: TestServices,
        project: Project
    ): List<PsiFile> {
        return testModule.files.map { testFile ->
            when {
                testFile.isKtFile -> {
                    val fileText = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                    KtTestUtil.createFile(testFile.name, fileText, project)
                }

                testFile.isJavaFile -> {
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

