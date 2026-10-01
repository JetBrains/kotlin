/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import java.nio.file.Path
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

@Serializable
internal class DiscoverScriptExtensionsOperationImpl(
    override val classpath: List<Path>,
    @SerialName("COMPILER_MESSAGE_RENDERER") internal var compilerMessageRenderer: CompilerMessageRenderer = DefaultCompilerMessageRenderer,
) : BuildOperationImpl<Collection<String>>(), DiscoverScriptExtensionsOperation, DiscoverScriptExtensionsOperation.Builder,
    DeepCopyable<DiscoverScriptExtensionsOperation> {

    override val usesApplicationEnvironment: Boolean
        get() = false

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): Collection<String> {
        // KT-84096 BTA: support daemon execution for script discovery operation
        check(executionPolicy is ExecutionPolicy.InProcess) { "Only in-process execution policy is supported for this operation." }
        val definitions = ScriptDefinitionsFromClasspathDiscoverySource(
            classpath.map(Path::toFile), defaultJvmScriptingHostConfiguration, KotlinLoggerMessageCollectorAdapter(
                logger ?: DefaultKotlinLogger, this[COMPILER_MESSAGE_RENDERER], warningsAsErrors = false
            ).reporter
        ).definitions

        return definitions.mapTo(arrayListOf()) { it.fileExtension }
    }

    override fun toBuilder(): DiscoverScriptExtensionsOperation.Builder = deepCopy()

    @UseFromImplModuleRestricted
    override fun <V> get(key: DiscoverScriptExtensionsOperation.Option<V>): V =
        DiscoverScriptExtensionsOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: DiscoverScriptExtensionsOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        DiscoverScriptExtensionsOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun build(): DiscoverScriptExtensionsOperation = deepCopy()

    override fun deepCopy(): DiscoverScriptExtensionsOperationImpl = DiscoverScriptExtensionsOperationImpl(classpath, compilerMessageRenderer)

    private operator fun <V> get(key: Option<V>): V =
        DiscoverScriptExtensionsOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    private operator fun <V> set(key: Option<V>, value: V) {
        DiscoverScriptExtensionsOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER")
    }

}
