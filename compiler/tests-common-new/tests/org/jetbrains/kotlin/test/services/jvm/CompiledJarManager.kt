/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

class CompiledJarManager(val testServices: TestServices) : TestService {
    private val jarCache = mutableMapOf<TestModule, File>()

    fun getCompiledJarForModule(module: TestModule): File {
        return jarCache.getOrPut(module) {
            val outputDir = testServices.createTempDirectory("module_${module.name}")
            val classFileFactory = testServices.dependencyProvider.getArtifact(module, ArtifactKinds.Jvm).classFileFactory
            val outputFileCollection = SimpleOutputFileCollection(classFileFactory.currentOutput)
            val messageCollector = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                .getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            outputFileCollection.writeAll(outputDir, messageCollector, reportOutputFiles = false)
            classFileFactory.releaseGeneratedOutput()
            outputDir
        }
    }
}

val TestServices.compiledJarManager: CompiledJarManager by TestServices.testServiceAccessor()
