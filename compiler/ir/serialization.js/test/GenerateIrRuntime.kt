/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibrarySearchPathResolver
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.resolver.impl.libraryResolver
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

fun buildConfiguration(environment: KotlinCoreEnvironment, moduleName: String): CompilerConfiguration {
    val runtimeConfiguration = environment.configuration.copy()
    runtimeConfiguration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
    runtimeConfiguration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

    runtimeConfiguration.languageVersionSettings = LanguageVersionSettingsImpl(
        LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
        specificFeatures = mapOf(
            LanguageFeature.AllowContractsForCustomFunctions to LanguageFeature.State.ENABLED,
            LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
        ),
        analysisFlags = mapOf(
            AnalysisFlags.useExperimental to listOf(
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.Experimental",
                "kotlin.ExperimentalMultiplatform"
            ),
            AnalysisFlags.allowResultReturnType to true
        )
    )

    return runtimeConfiguration
}

val environment = KotlinCoreEnvironment.createForTests(Disposable { }, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

fun createPsiFile(fileName: String): KtFile {
    val psiManager = PsiManager.getInstance(environment.project)
    val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

    return psiManager.findFile(file) as KtFile
}


fun buildKLib(
    moduleName: String,
    sources: List<String>,
    outputPath: String,
    allDependencies: KotlinLibraryResolveResult,
    commonSources: List<String>
) {
    generateKLib(
        project = environment.project,
        files = sources.map { source ->
            val file = createPsiFile(source)
            if (source in commonSources) {
                file.isCommonSource = true
            }
            file
        },
        configuration = buildConfiguration(environment, moduleName),
        allDependencies = allDependencies,
        friendDependencies = emptyList(),
        outputKlibPath = outputPath,
        nopack = true
    )
}

private fun listOfKtFilesFrom(paths: List<String>): List<String> {
    val currentDir = File("")
    return paths.flatMap { path ->
        File(path)
            .walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.relativeToOrSelf(currentDir).path }
            .asIterable()
    }.distinct()
}

fun main(args: Array<String>) {

    val inputFiles = mutableListOf<String>()
    var outputPath: String? = null
    val dependencies = mutableListOf<String>()
    val commonSources = mutableListOf<String>()

    var index = 0
    while (index < args.size) {
        val arg = args[index++]

        when (arg) {
            "-o" -> outputPath = args[index++]
            "-d" -> dependencies += args[index++]
            "-c" -> commonSources += args[index++]
            else -> inputFiles += arg
        }
    }

    if (outputPath == null) {
        error("Please set path to .klm file: `-o some/dir/module-name.klm`")
    }

    val dependencyAbsolutePaths = dependencies.map{ File(it).absolutePath }
    val unresolvedLibraries = dependencies.map { UnresolvedLibrary(it, null) }
    // Configure the resolver to only understand absolute path libraries.
    val libraryResolver = KotlinLibrarySearchPathResolver<KotlinLibrary>(
        repositories = emptyList(),
        directLibs = dependencyAbsolutePaths,
        distributionKlib = null,
        localKotlinDir = null,
        skipCurrentDir = true
        // TODO: pass logger attached to message collector here.
    ).libraryResolver()
    val resolvedLibraries = libraryResolver.resolveWithDependencies(unresolvedLibraries, true, true, true)

    buildKLib(File(outputPath).absolutePath, listOfKtFilesFrom(inputFiles), outputPath, resolvedLibraries, listOfKtFilesFrom(commonSources))
}
