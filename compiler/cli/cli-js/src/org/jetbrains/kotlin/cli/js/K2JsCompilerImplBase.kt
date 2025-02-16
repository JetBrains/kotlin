/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

internal abstract class K2JsCompilerImplBase(
    val arguments: K2JSCompilerArguments,
    val configuration: CompilerConfiguration,
    val moduleName: String,
    val outputName: String,
    val outputDir: File,
    val messageCollector: MessageCollector,
    val performanceManager: PerformanceManager?,
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

    abstract fun tryInitializeCompiler(rootDisposable: Disposable): KotlinCoreEnvironment?
}
