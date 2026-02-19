/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import java.nio.file.Path
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

internal class DiscoverScriptExtensionsOperationImpl private constructor(
    override val options: Options = Options(DiscoverScriptExtensionsOperation::class),
    override val classpath: List<Path>,
) : BuildOperationImpl<Collection<String>>(), DiscoverScriptExtensionsOperation, DiscoverScriptExtensionsOperation.Builder,
    DeepCopyable<DiscoverScriptExtensionsOperation> {

    constructor(classpath: List<Path>) : this(Options(DiscoverScriptExtensionsOperation::class), classpath)

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): Collection<String> {
        // KT-84096 BTA: support daemon execution for script discovery operation
        check(executionPolicy is ExecutionPolicy.InProcess) { "Only in-process execution policy is supported for this operation." }
        val definitions = ScriptDefinitionsFromClasspathDiscoverySource(
            classpath.map(Path::toFile), defaultJvmScriptingHostConfiguration, KotlinLoggerMessageCollectorAdapter(
                logger ?: DefaultKotlinLogger, this[COMPILER_MESSAGE_RENDERER]
            ).reporter
        ).definitions

        return definitions.mapTo(arrayListOf()) { it.fileExtension }
    }

    override fun toBuilder(): DiscoverScriptExtensionsOperation.Builder = deepCopy()

    @UseFromImplModuleRestricted
    override fun <V> get(key: DiscoverScriptExtensionsOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: DiscoverScriptExtensionsOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): DiscoverScriptExtensionsOperation = deepCopy()

    override fun deepCopy(): DiscoverScriptExtensionsOperationImpl = DiscoverScriptExtensionsOperationImpl(options.deepCopy(), classpath)

    companion object {
        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER", default = DefaultCompilerMessageRenderer)
    }

}