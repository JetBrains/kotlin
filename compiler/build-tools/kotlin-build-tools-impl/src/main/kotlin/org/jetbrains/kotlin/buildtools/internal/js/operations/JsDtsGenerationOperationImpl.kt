/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

internal class JsDtsGenerationOperationImpl private constructor(
    override val options: Options,
    override val klibs: List<Path>,
    override val outputDirectory: Path,
) : BuildOperationImpl<CompilationResult>(), JsDtsGenerationOperation, JsDtsGenerationOperation.Builder,
    DeepCopyable<JsDtsGenerationOperationImpl> {

    constructor(klibs: List<Path>, outputDirectory: Path) : this(
        options = Options(JsDtsGenerationOperation::class),
        klibs = klibs,
        outputDirectory = outputDirectory,
    ) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        // placeholder
        return CompilationResult.COMPILATION_SUCCESS
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsDtsGenerationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsDtsGenerationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun toBuilder(): JsDtsGenerationOperation.Builder = deepCopy()

    override fun build(): JsDtsGenerationOperation = deepCopy()

    override fun deepCopy(): JsDtsGenerationOperationImpl =
        JsDtsGenerationOperationImpl(options.deepCopy(), klibs, outputDirectory)

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        val MODULE_NAME: Option<String?> = Option("MODULE_NAME", null)
        val MODULE_KIND: Option<JsModuleKind?> = Option("MODULE_KIND", null)
        val GRANULARITY: Option<JsDtsGranularity?> = Option("GRANULARITY", null)
        val TS_COMPILATION_STRATEGY: Option<JsDtsCompilationStrategy?> = Option("TS_COMPILATION_STRATEGY", null)
        val COMPILE_LONG_AS_BIG_INT: Option<Boolean> = Option("COMPILE_LONG_AS_BIG_INT", false)
        val IMPLEMENT_INTERFACES: Option<Boolean> = Option("IMPLEMENT_INTERFACES", false)
        val EXPORT_SUSPEND_LAMBDAS: Option<Boolean> = Option("EXPORT_SUSPEND_LAMBDAS", false)
    }
}
