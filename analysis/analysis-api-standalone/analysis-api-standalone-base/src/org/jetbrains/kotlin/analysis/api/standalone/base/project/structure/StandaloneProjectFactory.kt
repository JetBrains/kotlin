/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Path

object StandaloneProjectFactory {
    fun createProjectEnvironment(
        projectDisposable: Disposable,
        applicationDisposable: Disposable
    ): KotlinCoreProjectEnvironment {
        val applicationEnvironment =
            KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(applicationDisposable, CompilerConfiguration())

        return KotlinCoreProjectEnvironment(projectDisposable, applicationEnvironment)
    }


    fun registerServicesForProjectEnvironment(
        environment: KotlinCoreProjectEnvironment,
        modules: List<KtModuleWithFiles>,
        languageVersionSettings: LanguageVersionSettings,
        jdkHome: Path?,
    ) {
        val project = environment.project

        KotlinCoreEnvironment.registerProjectExtensionPoints(project.extensionArea)
        KotlinCoreEnvironment.registerProjectServices(project)

        project.registerService(ProjectStructureProvider::class.java, KtStaticModuleProvider(modules.map { it.ktModule }))
        initialiseVirtualFinderFinderServices(modules, environment, jdkHome, languageVersionSettings)
        initialiseAnnotationServices(project)

        project.setupHighestLanguageLevel()
    }

    private fun initialiseAnnotationServices(project: MockProject) {
        project.registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
        project.registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())
    }

    private fun initialiseVirtualFinderFinderServices(
        modules: List<KtModuleWithFiles>,
        environment: KotlinCoreProjectEnvironment,
        jdkHome: Path?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        val project = environment.project

        val allSourceFiles = buildList {
            val files = modules.flatMap { it.files }
            addAll(files)
            addAll(findJvmRootsForJavaFiles(files.filterIsInstance<PsiJavaFile>()))
        }
        val allSourceFileRoots = allSourceFiles.map { JavaRoot(it.virtualFile, JavaRoot.RootType.SOURCE) }
        val libraryRoots = getAllBinaryRoots(modules.map { it.ktModule }, environment)
        libraryRoots.forEach { environment.addSourcesToClasspath(it.file) }

        val sourceAndLibraryRoots = buildList {
            addAll(libraryRoots)
            addAll(allSourceFileRoots)
        }

        val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
        val javaModuleFinder = CliJavaModuleFinder(jdkHome?.toFile(), null, javaFileManager, project, null)

        javaFileManager.initialize(
            JvmDependenciesIndexImpl(sourceAndLibraryRoots),
            listOf(createPackagePartsProvider(languageVersionSettings, project, libraryRoots)),
            SingleJavaFileRootsIndex(emptyList()),
            true
        )

        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(JavaModuleGraph(javaModuleFinder), emptyList(), javaModuleFinder.systemModules.toList(), project)
        )

        val finderFactory = CliVirtualFileFinderFactory(JvmDependenciesIndexImpl(sourceAndLibraryRoots), false)

        project.registerService(MetadataFinderFactory::class.java, finderFactory)
        project.registerService(VirtualFileFinderFactory::class.java, finderFactory)
    }

    private fun findJvmRootsForJavaFiles(files: List<PsiJavaFile>): List<PsiDirectory> {
        if (files.isEmpty()) return emptyList()
        val result = mutableSetOf<PsiDirectory>()
        for (file in files) {
            val packageParts = file.packageName.takeIf { it.isNotEmpty() }?.split('.') ?: emptyList()
            val javaDir = packageParts
                .reversed()
                .fold(file.parent) { dir, part ->
                    if (dir?.name == part) {
                        dir.parent
                    } else {
                        error("File package ${file.packageName} does not match file path ${file.virtualFile.path}")
                    }
                }
            result += javaDir as PsiDirectory
        }
        return result.toList()
    }

    fun getAllBinaryRoots(
        modules: List<KtModule>,
        environment: KotlinCoreProjectEnvironment
    ): List<JavaRoot> = withAllTransitiveDependencies(modules)
        .filterIsInstance<KtLibraryModule>()
        .flatMap { it.getBinaryRoots() }
        .map {
            val jar = environment.environment.jarFileSystem.findFileByPath(it.toAbsolutePath().toString() + "!/")!!
            JavaRoot(jar, JavaRoot.RootType.BINARY)
        }

    private fun withAllTransitiveDependencies(ktModules: List<KtModule>): List<KtModule> {
        val visited = hashSetOf<KtModule>()
        val stack = ktModules.toMutableList()
        while (stack.isNotEmpty()) {
            val module = stack.popLast()
            if (module in visited) continue
            visited += module
            for (dependency in module.allDependencies()) {
                if (dependency !in visited) {
                    stack += dependency
                }
            }
        }
        return visited.toList()
    }

    private fun KtModule.allDependencies(): List<KtModule> = buildList {
        addAll(allDirectDependencies())
        when (this) {
            is KtLibrarySourceModule -> {
                add(binaryLibrary)
            }
            is KtLibraryModule -> {
                addIfNotNull(librarySources)
            }
        }
    }


    fun createPackagePartsProvider(
        languageVersionSettings: LanguageVersionSettings,
        project: MockProject,
        libraryRoots: List<JavaRoot>
    ): JvmPackagePartProvider {
        return JvmPackagePartProvider(languageVersionSettings, ProjectScope.getLibrariesScope(project)).apply {
            addRoots(libraryRoots, MessageCollector.NONE)
            (ModuleAnnotationsResolver
                .getInstance(project) as CliModuleAnnotationsResolver)
                .addPackagePartProvider(this)
        }
    }
}