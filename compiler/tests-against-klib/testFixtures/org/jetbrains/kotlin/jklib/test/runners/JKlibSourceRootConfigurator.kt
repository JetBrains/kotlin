package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.cli.jklib.pipeline.JKLIB_OUTPUT_DESTINATION
import org.jetbrains.kotlin.config.JVMConfigurationKeys

import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import java.io.File

class JKlibSourceRootConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    // ... overrides ...

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        module.files.filter { it.name.endsWith(".kt") || it.name.endsWith(".kts") }.forEach { testFile ->
            val realFile = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile)
            configuration.addKotlinSourceRoot(realFile.absolutePath)
        }
        
        val stdlibKlib = System.getProperty("kotlin.stdlib.jvm.ir.klib")
        if (stdlibKlib != null) {
            configuration.put(JVMConfigurationKeys.KLIB_PATHS, listOf(stdlibKlib))
        }

        val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("klib-output")
        val outputFile = File(tempDir, "${module.name}.klib")
        configuration.put(JKLIB_OUTPUT_DESTINATION, outputFile.absolutePath)
    }
}
