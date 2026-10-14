/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.MessageReportingMessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import java.nio.file.Path
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

@Serializable
internal class DiscoverScriptExtensionsOperationImpl(
    override val classpath: List<Path>,
    @Transient
    @SerialName("COMPILER_MESSAGE_RENDERER") override var compilerMessageRenderer: CompilerMessageRenderer = DefaultCompilerMessageRenderer,
) : BuildOperationImpl<List<String>>(), DiscoverScriptExtensionsOperation, DiscoverScriptExtensionsOperation.Builder,
    DeepCopyable<DiscoverScriptExtensionsOperation> {

    override val usesApplicationEnvironment: Boolean
        get() = false

    @Transient
    private var compilerMessageCollector: MessageCollectorWithDiagnosticId? = null

    override fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase?) {
        super.afterDeserialization(messageReporter, compilerFacade)
        compilerMessageCollector = MessageReportingMessageCollectorWithDiagnosticId(messageReporter)
    }

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> {
        val messageVisitors = super.beforeSerialization(logger).toMutableList()
        messageVisitors.add(
            CompilerMessageVisitor(
                KotlinLoggerMessageCollectorAdapter(
                    logger,
                    compilerMessageRenderer,
                    false
                )
            )
        )
        return messageVisitors
    }

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): List<String> {
        // KT-84096 BTA: support daemon execution for script discovery operation
        check(executionPolicy is ExecutionPolicy.InProcess) { "Only in-process execution policy is supported for this operation." }
        val definitions = ScriptDefinitionsFromClasspathDiscoverySource(
            classpath.map(Path::toFile), defaultJvmScriptingHostConfiguration, KotlinLoggerMessageCollectorAdapter(
                logger ?: DefaultKotlinLogger, compilerMessageRenderer, warningsAsErrors = false
            ).reporter
        ).definitions

        return definitions.mapTo(arrayListOf()) { it.fileExtension }
    }

    override fun getResultSerializer(): KSerializer<List<String>> {
        return ListSerializer(String.serializer())
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

    override fun deepCopy(): DiscoverScriptExtensionsOperationImpl =
        DiscoverScriptExtensionsOperationImpl(classpath, compilerMessageRenderer).also {
            it.copyFrom(this)
        }

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
