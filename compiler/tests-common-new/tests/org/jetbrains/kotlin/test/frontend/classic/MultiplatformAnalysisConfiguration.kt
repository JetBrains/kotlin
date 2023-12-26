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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.ModulePath
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.test.directives.MultiplatformDiagnosticsDirectives.ENABLE_MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.types.typeUtil.closure

internal fun MultiplatformAnalysisConfiguration(testServices: TestServices): MultiplatformAnalysisConfiguration {
    return if (testServices.moduleStructure.allDirectives.contains(ENABLE_MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE)) {
        MultiplatformCompositeAnalysisConfiguration(
            testServices.dependencyProvider,
            testServices.sourceFileProvider,
            testServices.moduleDescriptorProvider,
        )
    } else {
        MultiplatformSeparateAnalysisConfiguration(
            testServices.dependencyProvider,
            testServices.sourceFileProvider,
            testServices.moduleDescriptorProvider
        )
    }
}

internal interface MultiplatformAnalysisConfiguration {
    fun getCompilerEnvironment(module: TestModule): TargetEnvironment
    fun getKtFilesForForSourceFiles(project: Project, module: TestModule): Map<TestFile, KtFile>
    fun getDependencyDescriptors(module: TestModule): List<ModuleDescriptor>
    fun getFriendDescriptors(module: TestModule): List<ModuleDescriptor>
    fun getDependsOnDescriptors(module: TestModule): List<ModuleDescriptor>
}

/**
 * Traditional 'compiler cli' configuration in which every platform gets analyzed 'separate'
 * by including dependsOn source files directly into the module descriptor.
 *
 * This mode works similar to how actual user projects would compile platforms like 'jvm', 'native' or js targets.
 */
internal class MultiplatformSeparateAnalysisConfiguration(
    private val dependencyProvider: DependencyProvider,
    private val sourceFileProvider: SourceFileProvider,
    private val moduleDescriptorProvider: ModuleDescriptorProvider
) : MultiplatformAnalysisConfiguration {

    override fun getCompilerEnvironment(module: TestModule): TargetEnvironment {
        return CompilerEnvironment
    }

    override fun getDependencyDescriptors(module: TestModule): List<ModuleDescriptor> {
        return getDescriptors(
            module.allDependencies - module.dependsOnDependencies.toSet(),
            dependencyProvider, moduleDescriptorProvider
        )
    }

    override fun getDependsOnDescriptors(module: TestModule): List<ModuleDescriptor> {
        return emptyList()
    }

    override fun getFriendDescriptors(module: TestModule): List<ModuleDescriptor> {
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
                val artifact = if (module.frontendKind == FrontendKinds.ClassicAndFIR) {
                    dependencyProvider.getArtifact(dependencyModule, FrontendKinds.ClassicAndFIR).k1Artifact
                } else {
                    dependencyProvider.getArtifact(dependencyModule, FrontendKinds.ClassicFrontend)
                }
                /*
                We need create KtFiles again with new project because otherwise we can access to some caches using
                old project as key which may leads to missing services in core environment
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
 * Configuration that will work 'more similar' to how the IDE works where dependsOn edges will be located
 * in separate module descriptors. The key difference to the IDE is, that this mode will not (yet) support
 * reversed depends on paths see [CompositeAnalysisModuleStructureOracle]
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

    override fun getDependencyDescriptors(module: TestModule): List<ModuleDescriptor> {
        // Transitive dependsOn descriptors should also be returned as dependencies
        val allDependsOnDependencies = module.dependsOnDependencies.closure(preserveOrder = true) { dependsOnDependency ->
            dependencyProvider.getTestModule(dependsOnDependency.moduleName).dependsOnDependencies
        }
        val allDependencies = (module.allDependencies + allDependsOnDependencies).distinct()
        return getDescriptors(allDependencies, dependencyProvider, moduleDescriptorProvider)
    }

    override fun getDependsOnDescriptors(module: TestModule): List<ModuleDescriptor> {
        return getDescriptors(module.dependsOnDependencies, dependencyProvider, moduleDescriptorProvider)
    }

    override fun getFriendDescriptors(module: TestModule): List<ModuleDescriptor> {
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
        /*
        This feature is not supported yet, since during testing of 'common modules',
        the 'less common modules' are not available as descriptors.
         */
        return emptyList()
    }

    override fun findAllDependsOnPaths(module: ModuleDescriptor): List<ModulePath> {
        return module.expectedByModules.flatMap { expectedByModule ->
            val head = listOf(module, expectedByModule)
            if (expectedByModule.expectedByModules.isEmpty()) listOf(ModulePath(head))
            else findAllDependsOnPaths(expectedByModule).map { path ->
                ModulePath(head + path.nodes)
            }
        }
    }
}

/* Utils */

private fun getDescriptors(
    dependencies: Iterable<DependencyDescription>,
    dependencyProvider: DependencyProvider,
    moduleDescriptorProvider: ModuleDescriptorProvider
): List<ModuleDescriptor> {
    return dependencies.filter { it.kind == DependencyKind.Source }
        .map { dependencyDescription -> dependencyProvider.getTestModule(dependencyDescription.moduleName) }
        .map { dependencyModule -> moduleDescriptorProvider.getModuleDescriptor(dependencyModule) }
}
