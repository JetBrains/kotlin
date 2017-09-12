/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.metadata

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File

class K2MetadataCompiler : CLICompiler<K2MetadataCompilerArguments>() {
    override fun createArguments() = K2MetadataCompilerArguments()

    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration, arguments: K2MetadataCompilerArguments, services: Services
    ) {
        // No specific arguments yet
    }

    override fun doExecute(
            arguments: K2MetadataCompilerArguments,
            configuration: CompilerConfiguration,
            rootDisposable: Disposable,
            paths: KotlinPaths?
    ): ExitCode {
        val collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg)
        }
        if (arguments.classpath != null) {
            configuration.addJvmClasspathRoots(arguments.classpath!!.split(File.pathSeparatorChar).map(::File))
        }

        configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)

        val destination = arguments.destination
        if (destination != null) {
            if (destination.endsWith(".jar")) {
                // TODO: support .jar destination
                collector.report(STRONG_WARNING, ".jar destination is not yet supported, results will be written to the directory with the given name")
            }
            configuration.put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, File(destination))
        }

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.METADATA_CONFIG_FILES)

        if (environment.getSourceFiles().isEmpty()) {
            if (arguments.version) {
                return ExitCode.OK
            }
            collector.report(ERROR, "No source files")
            return ExitCode.COMPILATION_ERROR
        }

        try {
            MetadataSerializer(true).serialize(environment)
        }
        catch (e: CompilationException) {
            collector.report(EXCEPTION, OutputMessageUtil.renderException(e), MessageUtil.psiElementToMessageLocation(e.element))
            return ExitCode.INTERNAL_ERROR
        }

        return ExitCode.OK
    }

    // TODO: update this once a launcher script for K2MetadataCompiler is available
    override fun executableScriptFileName(): String = "kotlinc"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2MetadataCompiler(), args)
        }
    }
}
