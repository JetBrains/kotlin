/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.DependencyRelation.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.transitiveDependsOnDependencies

class MetadataEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        if (WITH_STDLIB in module.directives) {
            configuration.add(
                CLIConfigurationKeys.CONTENT_ROOTS,
                JvmClasspathRoot(ForTestCompileRuntime.stdlibCommonForTests())
            )
        }

        configuration.moduleName = module.name
        configuration.metadataDestinationDirectory = testServices.compiledClassesManager.getOutputDirForModule(module)

        val friendDependencies = mutableListOf<String>()
        val refinesDependencies = mutableListOf<String>()

        fun processDependency(dependencyModule: TestModule, relation: DependencyRelation) {
            val dependencyOutputDir = testServices.compiledClassesManager.getOutputDirForModule(dependencyModule)
            configuration.addJvmClasspathRoot(dependencyOutputDir)
            when (relation) {
                RegularDependency -> {}
                FriendDependency -> friendDependencies += dependencyOutputDir.canonicalPath
                DependsOnDependency -> refinesDependencies += dependencyOutputDir.canonicalPath
            }
        }

        for (dependencyDescription in module.allDependencies.filter { it.relation != DependsOnDependency }) {
            processDependency(dependencyDescription.dependencyModule, dependencyDescription.relation)
        }
        for (dependencyModule in module.transitiveDependsOnDependencies()) {
            processDependency(dependencyModule, DependsOnDependency)
        }
        configuration.putIfNotNull(K2MetadataConfigurationKeys.FRIEND_PATHS, friendDependencies)
        configuration.putIfNotNull(K2MetadataConfigurationKeys.REFINES_PATHS, refinesDependencies)
        configuration.addKotlinSourceRoots(
            module.kotlinFiles.map { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it).canonicalPath }
        )
    }
}
