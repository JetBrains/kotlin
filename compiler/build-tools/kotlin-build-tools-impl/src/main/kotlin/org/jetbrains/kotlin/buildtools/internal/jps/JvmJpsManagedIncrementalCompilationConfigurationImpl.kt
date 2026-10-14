/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.internal.jps

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCache
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerTargetId
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.jps.incremental.CompilerIncrementalCompilationComponentsAdapter
import org.jetbrains.kotlin.buildtools.internal.jps.incremental.IncrementalCompilationComponentsAdapter
import org.jetbrains.kotlin.buildtools.internal.jps.incremental.trackers.*
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.BtaSerializable
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.RemoteIncrementalCompilationComponentsClient
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.nio.file.Path
import org.jetbrains.kotlin.buildtools.internal.BaseCompilationOperationImpl.Companion.LOOKUP_TRACKER as BASE_LOOKUP_TRACKER

/**
 * Marks a client-managed incremental compilation configuration, so that it can be recognized without mentioning any
 * `kotlin-build-tools-api-jps` type.
 *
 * That module is an optional dependency of this one: it is on the classpath only for consumers that use the JPS part of the API. Loading any
 * of its types when it is absent fails with a [NoClassDefFoundError], and so does loading
 * [JvmJpsManagedIncrementalCompilationConfigurationImpl], which implements its interfaces. This marker is declared here instead, where it is
 * always available, and only that class implements it, so an instance can exist only if api-jps is on the classpath.
 *
 * Therefore this declaration must not reference anything from `kotlin-build-tools-api-jps`.
 */
internal interface JvmClientManagedIncrementalCompilationConfiguration : JvmIncrementalCompilationConfiguration

@OptIn(DelicateBuildToolsApi::class)
internal fun JvmClientManagedIncrementalCompilationConfiguration.getClientManagedIncrementalCompilationOptions(
    reportCategories: Array<Int>,
    reportSeverity: Int,
): CompilationOptions {
    check(this is JvmJpsManagedIncrementalCompilationConfigurationImpl) {
        "Unexpected JPS incremental compilation configuration: ${this::class}. It must be an instance of JvmJpsManagedIncrementalCompilationConfigurationImpl."
    }

    return CompilationOptions(
        CompilerMode.JPS_COMPILER,
        CompileService.TargetPlatform.JVM,
        reportCategories,
        reportSeverity,
        requestedCompilationResults = emptyArray()
    )
}

@OptIn(DelicateBuildToolsApi::class, InternalBuildToolsApi::class)
context(operation: JvmCompilationOperationImpl)
internal fun JvmClientManagedIncrementalCompilationConfiguration.createCompilerServicesFacadeBase(
    logger: KotlinLogger,
    messageCollector: MessageCollectorWithDiagnosticId,
    compilationCanceledStatus: CompilationCanceledStatus,
): CompilerServicesFacadeBase {
    check(this is JvmJpsManagedIncrementalCompilationConfigurationImpl) {
        "Unexpected JPS incremental compilation configuration: ${this::class}. It must be an instance of JvmJpsManagedIncrementalCompilationConfigurationImpl."
    }

    val lookupTracker = lookupTracker?.let { tracker ->
        if (operation[BASE_LOOKUP_TRACKER] != null) {
            logger.warn(
                "A lookup tracker is set both as BaseCompilationOperation.LOOKUP_TRACKER and as " +
                        "JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER. " +
                        "The latter takes precedence."
            )
        }

        LookupTrackerAdapter(tracker)
    } ?: operation[BASE_LOOKUP_TRACKER]?.let(::LookupTrackerAdapter)

    return JpsManagedCompilerServicesWithResultsFacade(
        messageCollector,
        incrementalCompilationComponents = IncrementalCompilationComponentsAdapter(incrementalCompilationComponents),
        lookupTracker = lookupTracker,
        expectActualTracker = expectActualTracker?.let(::ExpectActualTrackerAdapter),
        inlineConstTracker = inlineConstTracker?.let(::InlineConstTrackerAdapter),
        enumWhenTracker = enumWhenTracker?.let(::EnumWhenTrackerAdapter),
        importTracker = importTracker?.let(::ImportTrackerAdapter),
        icFileMappingTracker = fileMappingTracker?.let(::FileMappingTrackerAdapter),
        compilationCanceledStatus = compilationCanceledStatus
    )
}

@OptIn(DelicateBuildToolsApi::class, InternalBuildToolsApi::class)
context(serviceBuilder: Services.Builder, operation: JvmCompilationOperationImpl)
internal fun JvmClientManagedIncrementalCompilationConfiguration.registerPlatformServices(logger: KotlinLogger) {
    check(this is JvmJpsManagedIncrementalCompilationConfigurationImpl) {
        "Unexpected JPS incremental compilation configuration: ${this::class}. It must be an instance of JvmJpsManagedIncrementalCompilationConfigurationImpl."
    }

    serviceBuilder.register(
        IncrementalCompilationComponents::class.java,
        IncrementalCompilationComponentsAdapter(this.incrementalCompilationComponents),
    )

    lookupTracker?.let { tracker ->
        if (operation[BASE_LOOKUP_TRACKER] != null) {
            logger.warn(
                "A lookup tracker is set both as BaseCompilationOperation.LOOKUP_TRACKER and as " +
                        "JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER. The latter takes precedence."
            )
        }

        serviceBuilder.register(LookupTracker::class.java, LookupTrackerAdapter(tracker))
    }

    importTracker?.let { tracker ->
        serviceBuilder.register(ImportTracker::class.java, ImportTrackerAdapter(tracker))
    }
    fileMappingTracker?.let { tracker ->
        serviceBuilder.register(ICFileMappingTracker::class.java, FileMappingTrackerAdapter(tracker))
    }
    expectActualTracker?.let { tracker ->
        serviceBuilder.register(ExpectActualTracker::class.java, ExpectActualTrackerAdapter(tracker))
    }
    enumWhenTracker?.let { tracker ->
        serviceBuilder.register(EnumWhenTracker::class.java, EnumWhenTrackerAdapter(tracker))
    }
    inlineConstTracker?.let { tracker ->
        serviceBuilder.register(InlineConstTracker::class.java, InlineConstTrackerAdapter(tracker))
    }
}

internal object CompilerIncrementalCompilationComponentsPlaceholder : CompilerIncrementalCompilationComponents {
    override fun getIncrementalCache(target: CompilerTargetId): CompilerIncrementalCache {
        error("Placeholder only for serialization")
    }
}

@DelicateBuildToolsApi
@OptIn(InternalBuildToolsApi::class)
@Serializable
internal class JvmJpsManagedIncrementalCompilationConfigurationImpl(
    @Transient
    override var incrementalCompilationComponents: CompilerIncrementalCompilationComponents = CompilerIncrementalCompilationComponentsPlaceholder,
) : JvmJpsManagedIncrementalCompilationConfiguration,
    JvmJpsManagedIncrementalCompilationConfiguration.Builder,
    JvmClientManagedIncrementalCompilationConfiguration,
    DeepCopyable<JvmJpsManagedIncrementalCompilationConfigurationImpl>,
    BtaSerializable {

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> {
        val messageVisitors = mutableListOf<MessageVisitor>()
        lookupTracker?.let { messageVisitors.add(LookupMessageVisitor(it)) }
        fileMappingTracker?.let { messageVisitors.add(FileMappingVisitor(it)) }
        expectActualTracker?.let { messageVisitors.add(ExpectActualVisitor(it)) }
        enumWhenTracker?.let { messageVisitors.add(EnumWhenVisitor(it)) }
        importTracker?.let { messageVisitors.add(ImportVisitor(it)) }
        inlineConstTracker?.let { messageVisitors.add(InlineConstVisitor(it)) }
        return messageVisitors
    }

    override fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase?) {
        if (hasLookupTracker) {
            lookupTracker = MessageReportingCompilerLookupTracker(messageReporter)
        }
        if (hasFileMappingTracker) {
            fileMappingTracker = MessageReportingCompilerFileMappingTracker(messageReporter)
        }
        if (hasExpectActualTracker) {
            expectActualTracker = MessageReportingCompilerExpectActualTracker(messageReporter)
        }
        if (hasEnumWhenTracker) {
            enumWhenTracker = MessageReportingCompilerEnumWhenTracker(messageReporter)
        }
        if (hasImportTracker) {
            importTracker = MessageReportingCompilerImportTracker(messageReporter)
        }
        if (hasInlineConstTracker) {
            inlineConstTracker = MessageReportingCompilerInlineConstTracker(messageReporter)
        }
        @Suppress("DEPRECATION")
        if (compilerFacade is CompilerCallbackServicesFacade) {
            incrementalCompilationComponents =
                CompilerIncrementalCompilationComponentsAdapter(RemoteIncrementalCompilationComponentsClient(compilerFacade))
        }
    }

    override fun build(): JvmJpsManagedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JvmJpsManagedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JvmJpsManagedIncrementalCompilationConfigurationImpl =
        JvmJpsManagedIncrementalCompilationConfigurationImpl(incrementalCompilationComponents).also {
            it.lookupTracker = lookupTracker
            it.fileMappingTracker = fileMappingTracker
            it.expectActualTracker = expectActualTracker
            it.enumWhenTracker = enumWhenTracker
            it.importTracker = importTracker
            it.inlineConstTracker = inlineConstTracker
        }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmJpsManagedIncrementalCompilationConfiguration.Option<V>): V =
        JvmJpsManagedIncrementalCompilationConfigurationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmJpsManagedIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        JvmJpsManagedIncrementalCompilationConfigurationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    @SerialName("LOOKUP_TRACKER")
    @Transient
    var lookupTracker: CompilerLookupTracker? = null
        set(value) {
            hasLookupTracker = value != null
            field = value
        }

    @SerialName("HAS_LOOKUP_TRACKER")
    private var hasLookupTracker: Boolean = false

    @SerialName("FILE_MAPPING_TRACKER")
    @Transient
    var fileMappingTracker: CompilerFileMappingTracker? = null
        set(value) {
            hasFileMappingTracker = value != null
            field = value
        }

    @SerialName("HAS_FILE_MAPPING_TRACKER")
    private var hasFileMappingTracker: Boolean = false

    @SerialName("EXPECT_ACTUAL_TRACKER")
    @Transient
    var expectActualTracker: CompilerExpectActualTracker? = null
        set(value) {
            hasExpectActualTracker = value != null
            field = value
        }

    @SerialName("HAS_EXPECT_ACTUAL_TRACKER")
    private var hasExpectActualTracker: Boolean = false

    @SerialName("ENUM_WHEN_TRACKER")
    @Transient
    var enumWhenTracker: CompilerEnumWhenTracker? = null
        set(value) {
            hasEnumWhenTracker = value != null
            field = value
        }

    @SerialName("HAS_ENUM_WHEN_TRACKER")
    private var hasEnumWhenTracker: Boolean = false

    @SerialName("IMPORT_TRACKER")
    @Transient
    var importTracker: CompilerImportTracker? = null
        set(value) {
            hasImportTracker = value != null
            field = value
        }

    @SerialName("HAS_IMPORT_TRACKER")
    private var hasImportTracker: Boolean = false

    @SerialName("INLINE_CONST_TRACKER")
    @Transient
    var inlineConstTracker: CompilerInlineConstTracker? = null
        set(value) {
            hasInlineConstTracker = value != null
            field = value
        }

    @SerialName("HAS_INLINE_CONST_TRACKER")
    private var hasInlineConstTracker: Boolean = false

    companion object {
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER")
        val FILE_MAPPING_TRACKER: Option<CompilerFileMappingTracker?> = Option("FILE_MAPPING_TRACKER")
        val EXPECT_ACTUAL_TRACKER: Option<CompilerExpectActualTracker?> = Option("EXPECT_ACTUAL_TRACKER")
        val ENUM_WHEN_TRACKER: Option<CompilerEnumWhenTracker?> = Option("ENUM_WHEN_TRACKER")
        val IMPORT_TRACKER: Option<CompilerImportTracker?> = Option("IMPORT_TRACKER")
        val INLINE_CONST_TRACKER: Option<CompilerInlineConstTracker?> = Option("INLINE_CONST_TRACKER")
    }
}

private class MessageReportingCompilerFileMappingTracker(private val messageReporter: (Message) -> Unit) : CompilerFileMappingTracker {
    override fun recordSourceFilesToOutputFileMapping(
        sourceFilePaths: Collection<Path>,
        outputFilePath: Path,
    ) {
        messageReporter(
            Message.FileMappingTracker.RecordSourceFilesToOutputFileMapping(sourceFilePaths.toList(), outputFilePath)
        )
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFilePath: Path) {
        messageReporter(
            Message.FileMappingTracker.RecordSourceReferencedByCompilerPlugin(sourceFilePath)
        )
    }

    override fun recordOutputFileGeneratedForPlugin(outputFilePath: Path) {
        messageReporter(
            Message.FileMappingTracker.RecordOutputFileGeneratedForPlugin(outputFilePath)
        )
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFilePath: Path) {
        messageReporter(
            Message.FileMappingTracker.RecordSourceFileGeneratedForPlugin(sourceFilePath)
        )
    }
}

private class MessageReportingCompilerExpectActualTracker(private val messageReporter: (Message) -> Unit) : CompilerExpectActualTracker {
    override fun report(expectFilePath: Path, actualFilePath: Path) {
        messageReporter(
            Message.ExpectActualTracker.Report(expectFilePath, actualFilePath)
        )
    }

    override fun reportExpectOfLenientStub(expectFilePath: Path) {
        messageReporter(
            Message.ExpectActualTracker.ReportExpectOfLenientStub(expectFilePath)
        )
    }
}

private class MessageReportingCompilerEnumWhenTracker(private val messageReporter: (Message) -> Unit) : CompilerEnumWhenTracker {
    override fun report(whenExpressionFilePath: Path, enumClassFqName: String) {
        messageReporter(
            Message.EnumWhenTracker.Report(whenExpressionFilePath, enumClassFqName)
        )
    }
}

private class MessageReportingCompilerImportTracker(private val messageReporter: (Message) -> Unit) : CompilerImportTracker {
    override fun report(filePath: Path, importedFqName: String) {
        messageReporter(
            Message.ImportTracker.Report(filePath, importedFqName)
        )
    }
}

private class MessageReportingCompilerInlineConstTracker(private val messageReporter: (Message) -> Unit) : CompilerInlineConstTracker {
    override fun report(
        filePath: Path,
        owner: String,
        name: String,
        constType: String,
    ) {
        messageReporter(
            Message.InlineConstTracker.Report(filePath, owner, name, constType)
        )
    }
}

//private class RemoteIncrementalCompilationComponentsClient(
//    @Suppress("DEPRECATION") val facade: CompilerCallbackServicesFacade,
//    val profiler: Profiler = DummyProfiler(),
//) : CompilerIncrementalCompilationComponents {
//    override fun getIncrementalCache(target: CompilerTargetId): CompilerIncrementalCache =
//        RemoteIncrementalCacheClient(facade, target, profiler)
//}
//
//private class RemoteIncrementalCacheClient(
//    @Suppress("DEPRECATION") val facade: CompilerCallbackServicesFacade,
//    val target: CompilerTargetId,
//    val profiler: Profiler = DummyProfiler(),
//) : CompilerIncrementalCache {
//    override fun getObsoletePackageParts(): Collection<String> =
//        profiler.withMeasure(this) { facade.incrementalCache_getObsoletePackageParts(target) }
//
//    override fun getObsoleteMultifileClasses(): Collection<String> =
//        profiler.withMeasure(this) { facade.incrementalCache_getObsoleteMultifileClassFacades(target) }
//
//    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? =
//        profiler.withMeasure(this) { facade.incrementalCache_getMultifileFacadeParts(target, facadeInternalName) }
//
//    override fun getPackagePartData(partInternalName: String): JvmPackagePartProto? =
//        profiler.withMeasure(this) { facade.incrementalCache_getPackagePartData(target, partInternalName) }
//
//    override fun getModuleMappingData(): ByteArray? = profiler.withMeasure(this) { facade.incrementalCache_getModuleMappingData(target) }
//
//    override fun getMetadata(fragmentName: String): Map<File, ByteArray> =
//        profiler.withMeasure(this) { facade.incrementalCache_getMetadata(target, fragmentName) }
//
//    override fun getClassFilePath(internalClassName: String): String =
//        profiler.withMeasure(this) { facade.incrementalCache_getClassFilePath(target, internalClassName) }
//
//    override fun close(): Unit = profiler.withMeasure(this) { facade.incrementalCache_close(target) }
//}
