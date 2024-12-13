/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

internal abstract class K2JsCompilerImplBase(
    val arguments: K2JSCompilerArguments,
    val configuration: CompilerConfiguration,
    val moduleName: String,
    val outputName: String,
    val outputDir: File,
    val messageCollector: MessageCollector,
    val performanceManager: CommonCompilerPerformanceManager?,
) {
    abstract fun checkTargetArguments(): ExitCode?

    abstract fun compileWithIC(
        icCaches: IcCachesArtifacts,
        targetConfiguration: CompilerConfiguration,
        moduleKind: ModuleKind?,
    ): ExitCode

    abstract fun compileNoIC(
        mainCallArguments: List<String>?,
        module: ModulesStructure,
        moduleKind: ModuleKind?,
    ): ExitCode

    abstract fun tryInitializeCompiler(libraries: List<String>, rootDisposable: Disposable): KotlinCoreEnvironment?

    protected fun initializeCommonConfiguration(libraries: List<String>) {
        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, libraries)

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        val hmppCliModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg), hmppCliModuleStructure?.getModuleNameForSource(arg))
        }

        arguments.relativePathBases?.let {
            configuration.put(KlibConfigurationKeys.KLIB_RELATIVE_PATH_BASES, it.toList())
        }

        configuration.put(KlibConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, arguments.normalizeAbsolutePath)
        configuration.put(KlibConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS, arguments.enableSignatureClashChecks)

        configuration.put(KlibConfigurationKeys.NO_DOUBLE_INLINING, arguments.noDoubleInlining)
        configuration.put(
            KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
            DuplicatedUniqueNameStrategy.parseOrDefault(
                arguments.duplicatedUniqueNameStrategy,
                default = DuplicatedUniqueNameStrategy.DENY
            )
        )
    }

}
