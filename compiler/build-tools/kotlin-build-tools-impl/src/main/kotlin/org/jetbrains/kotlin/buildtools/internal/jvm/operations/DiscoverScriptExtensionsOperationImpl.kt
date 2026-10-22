/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

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
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import java.nio.file.Path
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

@Serializable
internal class DiscoverScriptExtensionsOperationImpl(
    override val classpath: List<Path>,
    @Transient
    @SerialName("COMPILER_MESSAGE_RENDERER") override var compilerMessageRenderer: CompilerMessageRenderer = DefaultCompilerMessageRenderer,
) : BuildOperationImpl<Collection<String>>(), DiscoverScriptExtensionsOperation, DiscoverScriptExtensionsOperation.Builder,
    DeepCopyable<DiscoverScriptExtensionsOperation>,
    SupportsNewDaemonMode<Collection<String>> {

    override val usesApplicationEnvironment: Boolean
        get() = false

    @Transient
    private var compilerMessageCollector: MessageCollectorWithDiagnosticId? = null

    override fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase?) {
        super.afterDeserialization(messageReporter, compilerFacade)
        compilerMessageCollector = MessageReportingMessageCollectorWithDiagnosticId(messageReporter)
    }

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> = buildList {
        addAll(super.beforeSerialization(logger))
        add(
            CompilerMessageVisitor(
                KotlinLoggerMessageCollectorAdapter(
                    logger,
                    compilerMessageRenderer,
                    false
                )
            )
        )
    }

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): Collection<String> {
        check(executionPolicy is ExecutionPolicy.InProcess) { "Only in-process execution policy is supported for this operation." }
        val definitions = ScriptDefinitionsFromClasspathDiscoverySource(
            classpath.map(Path::toFile), defaultJvmScriptingHostConfiguration, KotlinLoggerMessageCollectorAdapter(
                logger ?: DefaultKotlinLogger, compilerMessageRenderer, warningsAsErrors = false
            ).reporter
        ).definitions

        return definitions.mapTo(arrayListOf()) { it.fileExtension }
    }

    override fun serializeResult(result: Collection<String>): SerializedDataContainer {
        return KotlinToolchainsImpl.serializer.serialize(ListSerializer(String.serializer()), result.toList())
    }

    override fun deserializeResult(serializedData: SerializedDataContainer): Collection<String> {
        return KotlinToolchainsImpl.serializer.deserialize(ListSerializer(String.serializer()), serializedData)
    }

    override fun toBuilder(): DiscoverScriptExtensionsOperation.Builder = deepCopy()

    @UseFromImplModuleRestricted
    override fun <V> get(key: DiscoverScriptExtensionsOperation.Option<V>): V = getPropertyByName(propertyAccessMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: DiscoverScriptExtensionsOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        setPropertyByName(propertyAccessMap, key.id, value)
    }

    override fun build(): DiscoverScriptExtensionsOperation = deepCopy()

    override fun deepCopy(): DiscoverScriptExtensionsOperationImpl =
        DiscoverScriptExtensionsOperationImpl(classpath.toList(), compilerMessageRenderer).also { it.copyFrom(this) }

    companion object {
        private val propertyAccessMap = mapOf(
            "COMPILER_MESSAGE_RENDERER" to DiscoverScriptExtensionsOperationImpl::compilerMessageRenderer
        )
    }
}
