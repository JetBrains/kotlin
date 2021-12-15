/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TestFunctionName")

package org.jetbrains.kotlin.test.frontend.classic

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.ModulePath
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.test.directives.MultiplatformDiagnosticsDirectives.MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

internal fun MultiplatformAnalysisConfiguration(testServices: TestServices): MultiplatformAnalysisConfiguration {
    return if (testServices.moduleStructure.allDirectives.contains(MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE))
        MultiplatformCompositeAnalysisConfiguration(
            testServices.dependencyProvider,
            testServices.sourceFileProvider,
            testServices.moduleDescriptorProvider,
        )
    else MultiplatformSeparateAnalysisConfiguration(
        testServices.dependencyProvider,
        testServices.sourceFileProvider,
        testServices.moduleDescriptorProvider
    )
}

internal interface MultiplatformAnalysisConfiguration {
    fun getCompilerEnvironment(module: TestModule): TargetEnvironment
    fun getKtFilesForForSourceFiles(project: Project, module: TestModule): Map<TestFile, KtFile>
    fun getDependencyDescriptors(module: TestModule): List<ModuleDescriptorImpl>
    fun getFriendDescriptors(module: TestModule): List<ModuleDescriptorImpl>
    fun getDependsOnDescriptors(module: TestModule): List<ModuleDescriptorImpl>
}

/**
 * Traditional 'compiler cli' configuration...
 */
internal class MultiplatformSeparateAnalysisConfiguration(
    private val dependencyProvider: DependencyProvider,
    private val sourceFileProvider: SourceFileProvider,
    private val moduleDescriptorProvider: ModuleDescriptorProvider
) : MultiplatformAnalysisConfiguration {

    override fun getCompilerEnvironment(module: TestModule): TargetEnvironment {
        return CompilerEnvironment
    }

    override fun getDependencyDescriptors(module: TestModule): List<ModuleDescriptorImpl> {
        return getDescriptors(
            module.allDependencies - module.dependsOnDependencies.toSet(),
            dependencyProvider, moduleDescriptorProvider
        )
    }

    override fun getDependsOnDescriptors(module: TestModule): List<ModuleDescriptorImpl> {
        return emptyList()
    }

    override fun getFriendDescriptors(module: TestModule): List<ModuleDescriptorImpl> {
        return getDescriptors(
            module.friendDependencies, dependencyProvider, moduleDescriptorProvider
        )
    }

    override fun getKtFilesForForSourceFiles(project: Project, module: TestModule): Map<TestFile, KtFile> {
        val ktFilesMap = sourceFileProvider.getKtFilesForSourceFiles(module.files, project).toMutableMap()
        fun addDependsOnSources(dependencies: List<DependencyDescription>) {
            if (dependencies.isEmpty()) return
            for (dependency in dependencies) {
                val dependencyModule = dependencyProvider.getTestModule(dependency.moduleName)
                val artifact = dependencyProvider.getArtifact(dependencyModule, FrontendKinds.ClassicFrontend)
                /*
                * We need create KtFiles again with new project because otherwise we can access to some caches using
                *   old project as key which may leads to missing services in core environment
                */
                val ktFiles = sourceFileProvider.getKtFilesForSourceFiles(artifact.allKtFiles.keys, project)
                ktFiles.values.forEach { ktFile -> ktFile.isCommonSource = true }
                ktFilesMap.putAll(ktFiles)
                addDependsOnSources(dependencyModule.dependsOnDependencies)
            }
        }
        addDependsOnSources(module.dependsOnDependencies)
        return ktFilesMap
    }
}

/**
 * Metadata / IDE like configuration...
 */
internal class MultiplatformCompositeAnalysisConfiguration(
    private val dependencyProvider: DependencyProvider,
    private val sourceFileProvider: SourceFileProvider,
    private val moduleDescriptorProvider: ModuleDescriptorProvider,
) : MultiplatformAnalysisConfiguration {

    override fun getCompilerEnvironment(module: TestModule): TargetEnvironment {
        return CompositeAnalysisTargetEnvironment
    }

    override fun getKtFilesForForSourceFiles(project: Project, module: TestModule): Map<TestFile, KtFile> {
        return sourceFileProvider.getKtFilesForSourceFiles(module.files, project)
    }

    override fun getDependencyDescriptors(module: TestModule): List<ModuleDescriptorImpl> {
        return getDescriptors(module.allDependencies, dependencyProvider, moduleDescriptorProvider)
    }

    override fun getDependsOnDescriptors(module: TestModule): List<ModuleDescriptorImpl> {
        return getDescriptors(module.dependsOnDependencies, dependencyProvider, moduleDescriptorProvider)
    }

    override fun getFriendDescriptors(module: TestModule): List<ModuleDescriptorImpl> {
        return getDescriptors(module.friendDependencies, dependencyProvider, moduleDescriptorProvider)
    }
}

private object CompositeAnalysisTargetEnvironment : TargetEnvironment("Test: Multiplatform with Composite Analysis") {
    override fun configure(container: StorageComponentContainer) {
        CompilerEnvironment.configure(container)
        container.useInstance(CompositeAnalysisModuleStructureOracle)
    }
}

private object CompositeAnalysisModuleStructureOracle : ModuleStructureOracle {

    override fun hasImplementingModules(module: ModuleDescriptor): Boolean {
        return findAllReversedDependsOnPaths(module).isNotEmpty()
    }

    override fun findAllReversedDependsOnPaths(module: ModuleDescriptor): List<ModulePath> {
        return emptyList() // <- Not supported, yet
    }

    override fun findAllDependsOnPaths(module: ModuleDescriptor): List<ModulePath> {
        return module.expectedByModules.flatMap { expectedByModule ->
            if (expectedByModule.expectedByModules.isEmpty()) listOf(ModulePath(listOf(expectedByModule)))
            else findAllDependsOnPaths(expectedByModule).map { path ->
                ModulePath(listOf(expectedByModule) + path.nodes)
            }
        }
    }
}

/* Utils */

private fun getDescriptors(
    dependencies: Iterable<DependencyDescription>,
    dependencyProvider: DependencyProvider,
    moduleDescriptorProvider: ModuleDescriptorProvider
): List<ModuleDescriptorImpl> {
    return dependencies.filter { it.kind == DependencyKind.Source }
        .map { dependencyDescription -> dependencyProvider.getTestModule(dependencyDescription.moduleName) }
        .map { dependencyModule -> moduleDescriptorProvider.getModuleDescriptor(dependencyModule) }
}
