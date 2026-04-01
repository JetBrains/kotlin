/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.configuration

import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.kotlinFiles
import org.jetbrains.kotlin.test.services.service
import java.io.File

class AnalysisApiJvmEnvironmentConfigurator(testServices: TestServices) : JvmEnvironmentConfigurator(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = super.additionalServices + listOf(
            service(::CompiledLibraryProvider),
        )

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)

        val libraryJars = module.directives[AnalysisApiTestDirectives.ATTACH_LIBRARY_JAR]
        if (libraryJars.isNotEmpty()) {
            val sourceFileParents = module.kotlinFiles.map { it.originalFile.parentFile }.distinct()

            nextJar@ for (jarName in libraryJars) {
                for (sourceFileParent in sourceFileParents) {
                    val targetFile = sourceFileParent.resolve(jarName)
                    if (targetFile.exists()) {
                        configuration.addJvmClasspathRoot(targetFile)
                        continue@nextJar
                    }
                }

                error("Library file $jarName not found")
            }
        }
    }

    override fun convertDependencyToFileList(dependency: DependencyDescription): List<File> {
        val friendModule = dependency.dependencyModule
        testServices.compiledLibraryProvider.getCompiledLibrary(friendModule.name)?.roots?.let {
            return it.map { it.toFile() }
        }
        return super.convertDependencyToFileList(dependency)
    }
}
