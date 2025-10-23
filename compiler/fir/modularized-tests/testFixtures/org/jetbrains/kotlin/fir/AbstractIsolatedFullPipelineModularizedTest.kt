/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.io.path.createTempDirectory

/*
 * This class contains the isolated execution of a single module (compilation and measurements),
 * extracted from AbstractFullPipelineModularizedTest#processModule together with its
 * implementation dependencies. AbstractFullPipelineModularizedTest now delegates to an instance
 * of this class.
 */
open class AbstractIsolatedFullPipelineModularizedTest(private val config: ModularizedTestConfig) {

    // Some tests may require resolving classpath for known plugins from the Kotlin distribution
    private val composePluginClasspath: List<String>? = config.composePluginClasspath?.split(File.pathSeparator)
    private val kotlinHome = config.kotlinHome?.let { KotlinPathsFromHomeDir(File(it)) } ?: PathUtil.kotlinPathsForDistDirectoryForTests

    fun runSingleModelCompilation(
        modelPath: String, tempDir: File? = null,
        configureArguments: (K2JVMCompilerArguments) -> Unit = {},
    ): Pair<ExitCode, MessageCollectorImpl> {
        val outputDir = createTempDirectory(tempDir?.toPath(), "compile-output").toFile()
        if (tempDir == null) outputDir.deleteOnExit()
        val moduleData = loadModuleDumpFile(File(modelPath), config).single()
        val messageCollector = MessageCollectorImpl()
        val result = processModule(moduleData, outputDir, messageCollector, null, configureArguments)
        return result to messageCollector
    }

    fun processModule(
        moduleData: ModuleData, outputDir: File, messageCollector: MessageCollector, performanceManager: PerformanceManager?,
        configureArguments: (K2JVMCompilerArguments) -> Unit = {},
    ): ExitCode {
        val compiler = K2JVMCompiler()
        val args = compiler.createArguments()
        configureBaseArguments(args, moduleData, outputDir)
        configureArguments(args)

        val services = Services.Builder().also {
            if (performanceManager != null) it.register(PerformanceManager::class.java, performanceManager)
        }.build()

        return try {
            compiler.exec(messageCollector, services, args)
        } catch (e: Exception) {
            e.printStackTrace()
            ExitCode.INTERNAL_ERROR
        }
    }

    private fun substituteCompilerPluginPathForKnownPlugins(path: String): File? {
        val file = File(path)
        return when {
            file.name.startsWith("kotlinx-serialization") || file.name.startsWith("kotlin-serialization") ->
                kotlinHome.jar(KotlinPaths.Jar.SerializationPlugin)
            file.name.startsWith("kotlin-sam-with-receiver") -> kotlinHome.jar(KotlinPaths.Jar.SamWithReceiver)
            file.name.startsWith("kotlin-allopen") -> kotlinHome.jar(KotlinPaths.Jar.AllOpenPlugin)
            file.name.startsWith("kotlin-noarg") -> kotlinHome.jar(KotlinPaths.Jar.NoArgPlugin)
            file.name.startsWith("kotlin-lombok") -> kotlinHome.jar(KotlinPaths.Jar.LombokPlugin)
            file.name.startsWith("kotlin-compose-compiler-plugin") -> {
                // compose plugin is not a part of the dist yet, so we have to go an extra mile to get it
                composePluginClasspath?.firstOrNull()?.let(::File)
            }
            // Assuming that the rest is the custom compiler plugins, that cannot be kept stable with the new compiler, so we're skipping them
            // If the module is compillable without it - fine, otherwise at least it will hopefully be a stable failure.
            else -> null
        }
    }

    private fun configureBaseArguments(args: K2JVMCompilerArguments, moduleData: ModuleData, outputDir: File) {
        val originalArguments = moduleData.arguments as? K2JVMCompilerArguments
        if (originalArguments != null) {
            args.apiVersion = originalArguments.apiVersion
            args.noJdk = originalArguments.noJdk
            args.noStdlib = originalArguments.noStdlib
            args.noReflect = originalArguments.noReflect
            args.jvmTarget = originalArguments.jvmTargetIfSupported()?.description
            args.jsr305 = originalArguments.jsr305
            args.nullabilityAnnotations = originalArguments.nullabilityAnnotations
            args.jspecifyAnnotations = originalArguments.jspecifyAnnotations
            @Suppress("DEPRECATION")
            args.jvmDefault = originalArguments.jvmDefault
            args.jvmDefaultStable = originalArguments.jvmDefaultStable
            args.jdkRelease = originalArguments.jdkRelease
            args.progressiveMode = originalArguments.progressiveMode
            args.optIn = (moduleData.optInAnnotations + (originalArguments.optIn ?: emptyArray())).toTypedArray()
            args.allowKotlinPackage = originalArguments.allowKotlinPackage

            args.pluginOptions = originalArguments.pluginOptions
            args.pluginClasspaths = originalArguments.pluginClasspaths?.mapNotNull {
                substituteCompilerPluginPathForKnownPlugins(it)?.absolutePath
            }?.toTypedArray()
            args.contextReceivers = originalArguments.contextReceivers
            args.contextParameters = originalArguments.contextParameters
            args.multiDollarInterpolation = originalArguments.multiDollarInterpolation
            args.skipPrereleaseCheck = originalArguments.skipPrereleaseCheck
            args.whenGuards = originalArguments.whenGuards
            args.nestedTypeAliases = originalArguments.nestedTypeAliases

        } else {
            args.jvmTarget = config.jvmTarget
            args.allowKotlinPackage = true
        }
        args.reportPerf = true
        args.jdkHome = moduleData.jdkHome?.absolutePath ?: originalArguments?.jdkHome?.fixPath(config.rootPathPrefix)?.absolutePath
        args.renderInternalDiagnosticNames = true
        args.debugLevelCompilerChecks = config.enableSlowAssertions
        configureArgsUsingBuildFile(args, moduleData, outputDir)
    }

    private fun configureArgsUsingBuildFile(args: K2JVMCompilerArguments, moduleData: ModuleData, outputDir: File) {
        val builder = KotlinModuleXmlBuilder()
        builder.addModule(
            moduleData.name,
            outputDir.absolutePath,
            sourceFiles = moduleData.sources,
            javaSourceRoots = moduleData.javaSourceRoots.map { JvmSourceRoot(it.path, it.packagePrefix) },
            classpathRoots = moduleData.classpath,
            commonSourceFiles = emptyList(),
            modularJdkRoot = moduleData.modularJdkRoot,
            "java-production",
            isTests = false,
            emptySet(),
            friendDirs = moduleData.friendDirs,
            isIncrementalCompilation = true
        )
        val modulesFile = outputDir.resolve("modules.xml")
        modulesFile.writeText(builder.asText().toString())
        args.buildFile = modulesFile.absolutePath
    }
}

internal fun K2JVMCompilerArguments.jvmTargetIfSupported(): JvmTarget? {
    val specified = jvmTarget?.let { JvmTarget.fromString(it) } ?: return null
    if (specified != JvmTarget.JVM_1_6) return specified
    return null
}

