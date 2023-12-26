/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

class CompiledClassesManager(val testServices: TestServices) : TestService {
    private val compiledKotlinCache = mutableMapOf<TestModule, File>()
    private val compiledJavaCache = mutableMapOf<TestModule, File>()
    var specifiedFrontendKind: FrontendKind<*> = FrontendKind.NoFrontend

    fun getCompiledKotlinDirForModule(module: TestModule, classFileFactory: ClassFileFactory? = null): File {
        return compiledKotlinCache.getOrPut(module) {
            val outputDir = testServices.getOrCreateTempDirectory("module_${module.name}_kotlin-classes")

            @Suppress("NAME_SHADOWING")
            val classFileFactory = classFileFactory ?: if (module.binaryKind == ArtifactKinds.JvmFromK1AndK2) {
                require(specifiedFrontendKind == FrontendKinds.FIR || specifiedFrontendKind == FrontendKinds.ClassicFrontend)
                val k1AndK2Artifact = testServices.dependencyProvider.getArtifact(module, ArtifactKinds.JvmFromK1AndK2)
                if (specifiedFrontendKind == FrontendKinds.FIR) {
                    k1AndK2Artifact.fromK2.classFileFactory
                } else {
                    k1AndK2Artifact.fromK1.classFileFactory
                }
            } else {
                testServices.dependencyProvider.getArtifact(module, ArtifactKinds.Jvm).classFileFactory
            }
            val outputFileCollection = SimpleOutputFileCollection(classFileFactory.currentOutput)
            val messageCollector = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                .getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            outputFileCollection.writeAll(outputDir, messageCollector, reportOutputFiles = false)
            outputDir
        }
    }

    fun getCompiledJavaDirForModule(module: TestModule): File? {
        return compiledJavaCache[module]
    }

    fun getOrCreateCompiledJavaDirForModule(module: TestModule): File {
        return compiledJavaCache.getOrPut(module) {
            testServices.getOrCreateTempDirectory("module_${module.name}_java-classes")
        }
    }
}

val TestServices.compiledClassesManager: CompiledClassesManager by TestServices.testServiceAccessor()