/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.config.fileMappingTracker
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

class CompiledClassesManager(val testServices: TestServices) : TestService {
    private val outputDirCache = mutableMapOf<TestModule, File>()

    fun compileKotlinToDiskAndGetOutputDir(module: TestModule, classFileFactory: ClassFileFactory?): File {
        val outputDir = getOutputDirForModule(module)
        val classFileFactory = classFileFactory ?: testServices.artifactsProvider.getArtifact(module, ArtifactKinds.Jvm).classFileFactory
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
        classFileFactory.writeAll(
            outputDir,
            configuration,
            reportOutputFiles = false,
            configuration.fileMappingTracker
        )
        return outputDir
    }

    fun getOutputDirForModule(module: TestModule): File {
        return outputDirCache.getOrPut(module) {
            testServices.getOrCreateTempDirectory("module_${module.name}_classes")
        }
    }
}

val TestServices.compiledClassesManager: CompiledClassesManager by TestServices.testServiceAccessor()
