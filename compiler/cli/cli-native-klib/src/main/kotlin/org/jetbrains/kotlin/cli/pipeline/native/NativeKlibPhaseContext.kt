/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * A minimal [PhaseContext] implementation for the native klib CLI pipeline.
 */
class NativeKlibPhaseContext(
    override val config: NativeKlibCompilationConfig,
    private val disposable: Disposable,
    override val messageCollector: MessageCollector,
    override val performanceManager: PerformanceManager?,
) : PhaseContext {
    override var inVerbosePhase: Boolean = false

    override fun dispose() {
        // Disposed by the pipeline
    }
}

/**
 * A minimal [NativeKlibCompilationConfig] implementation for the native klib CLI pipeline.
 */
class NativeKlibConfig(
    override val project: Project,
    override val configuration: CompilerConfiguration,
    override val target: KonanTarget,
    override val resolvedLibraries: KotlinLibraryResolveResult,
    override val moduleId: String,
    override val manifestProperties: Properties?,
) : NativeKlibCompilationConfig
