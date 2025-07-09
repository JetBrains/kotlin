/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.pipeline.web.computeOutputKlibPath
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature.MultiPlatformProjects
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.SourcesKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.test.services.isLeafModuleInMppGraph
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.transitiveDependsOnDependencies

/**
 * This is a test facade created specifically for running KLIB backward-compatibility tests.
 * The goal of such tests is to guarantee the ability for the new compiler to read KLIBs produced
 * by the older released versions of the compiler.
 *
 * See also https://kotlinlang.org/docs/kotlin-evolution-principles.html#kotlin-klib-binaries
 *
 * The sources are compiled by the old (custom) compiler using an implementation of
 * [CustomKlibCompilerFirstPhaseFacade]. Then all produced KLIBs are passed to the new (current)
 * compiler to produce the executable binary. Finally, the binary is executed and the execution
 * result is verified.
 *
 * This facade effectively replaces a few smaller facades, and allows directly transforming
 * [ResultingArtifact.Source] artifacts to [BinaryArtifacts.KLib] artifacts.
 */
abstract class CustomKlibCompilerFirstPhaseFacade(
    val testServices: TestServices,
) : AbstractTestFacade<ResultingArtifact.Source, BinaryArtifacts.KLib>() {

    protected abstract val TestModule.customKlibCompilerDefaultLanguageVersion: LanguageVersion

    final override val inputKind get() = SourcesKind
    final override val outputKind get() = ArtifactKinds.KLib

    final override fun shouldTransform(module: TestModule): Boolean {
        return if (module.languageVersionSettings.supportsFeature(MultiPlatformProjects)) {
            module.isLeafModuleInMppGraph(testServices)
        } else {
            true
        }
    }

    final override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): BinaryArtifacts.KLib {
        // Pass custom language features as arguments to the compiler invocation.
        val customArgs = module.languageVersionSettings.getCustomizedLanguageFeatures().entries.mapNotNull { (feature, state) ->
            val featureSinceVersion = feature.sinceVersion

            if (featureSinceVersion == null || featureSinceVersion < module.customKlibCompilerDefaultLanguageVersion) {
                // Pass only those language features that were already stable in the custom compiler version.
                // Otherwise, we might have a false positively ignored (grey) test because the custom compiler does not know
                // the specified language feature and the compilation ends with an error, but not because the compiler actually
                // failed to compile the source code.
                return@mapNotNull null
            }

            "-XXLanguage:${if (state == LanguageFeature.State.ENABLED) "+" else "-"}${feature.name}"
        }

        val isKmpSupported = module.languageVersionSettings.supportsFeature(MultiPlatformProjects)
        val modulesToCompile: List<TestModule> = if (isKmpSupported)
            module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)
        else
            listOf(module)

        val sourceFileProvider = testServices.sourceFileProvider
        val filesToCompile: List<String> = modulesToCompile.flatMap { it.files }
            .filter { it.isKtFile }
            .map { sourceFileProvider.getOrCreateRealFileForSourceFile(it).absolutePath }

        val (regularDependencies: Set<String>, friendDependencies: Set<String>) = collectDependencies(module)

        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val outputKlibPath: String = compilerConfiguration.computeOutputKlibPath()

        return compileKlib(
            module = module,
            customArgs = customArgs,
            sources = filesToCompile,
            regularDependencies = regularDependencies,
            friendDependencies = friendDependencies,
            outputKlibPath = outputKlibPath,
        )
    }

    /** Returns the set of regular dependency paths and the set of friend dependency paths. */
    protected abstract fun collectDependencies(module: TestModule): Pair<Set<String>, Set<String>>

    /** Invokes a custom KLIB compiler. */
    protected abstract fun compileKlib(
        module: TestModule,
        customArgs: List<String>,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib
}
