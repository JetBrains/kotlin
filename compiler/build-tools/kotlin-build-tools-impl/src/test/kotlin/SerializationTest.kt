@file:OptIn(ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.*
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.internal.serializability.KlibTargetIdSerializer
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SerializationTest {

    interface Serializer {
        fun <T : Any> serializeAndDeserialize(value: T, serializer: KSerializer<T>): T

        object Protobuf : Serializer {
            private val protoBuf = ProtoBuf { serializersModule = btaSerializersModule }

            override fun <T : Any> serializeAndDeserialize(value: T, serializer: KSerializer<T>): T {
                val byteArray = protoBuf.encodeToByteArray(serializer, value)
                @Suppress("UNCHECKED_CAST")
                return protoBuf.decodeFromByteArray(serializer, byteArray)
            }
        }
    }

    inline fun <reified T : Any> Serializer.serializeAndDeserialize(value: T): T = serializeAndDeserialize(value, serializer<T>())

    val serializer = Serializer.Protobuf

    @Test
    @DisplayName("Argument classes are serialized correctly")
    fun argumentClasses() {
        val jvmArguments = JvmCompilerArgumentsImpl()
        jvmArguments.jdkHome = Path("/someBuildFile.xml")
        jvmArguments.jvmDefault = JvmDefaultMode.NO_COMPATIBILITY.stringValue
        val jvmArguments2 = serializer.serializeAndDeserialize<JvmCompilerArgumentsImpl>(jvmArguments)
        assertEquals(jvmArguments.jdkHome, jvmArguments2.jdkHome)
        assertEquals(jvmArguments.jvmDefault, jvmArguments2.jvmDefault)
        assertEquals(jvmArguments.toArgumentStrings(), jvmArguments2.toArgumentStrings())
    }

    @Test
    @DisplayName("JS compiler arguments are serialized correctly")
    fun jsArgumentsSerialization() {
        val jsArgs = JsArgumentsImpl().apply {
            sourceMap = true
            moduleKind = JsModuleKind.ES
            friendModules = listOf(Path("/path/to/friend.klib"))
        }
        val deserialized = serializer.serializeAndDeserialize(jsArgs)
        assertEquals(jsArgs.sourceMap, deserialized.sourceMap)
        assertEquals(jsArgs.moduleKind, deserialized.moduleKind)
        assertEquals(jsArgs.friendModules, deserialized.friendModules)
        assertEquals(jsArgs.toArgumentStrings(), deserialized.toArgumentStrings())
    }

    @Test
    @DisplayName("Wasm compiler arguments are serialized correctly")
    fun wasmArgumentsSerialization() {
        val wasmArgs = WasmArgumentsImpl().apply {
            wasmTarget = WasmTarget.WASM_JS
            wasmEnableArrayRangeChecks = true
        }
        val deserialized = serializer.serializeAndDeserialize(wasmArgs)
        assertEquals(wasmArgs.wasmTarget, deserialized.wasmTarget)
        assertEquals(wasmArgs.wasmEnableArrayRangeChecks, deserialized.wasmEnableArrayRangeChecks)
        assertEquals(wasmArgs.toArgumentStrings(), deserialized.toArgumentStrings())
    }

    @Test
    @DisplayName("Metadata compiler arguments are serialized correctly")
    fun metadataArgumentsSerialization() {
        val metadataArgs = MetadataArgumentsImpl().apply {
            destination = "/path/to/output"
            classpath = listOf(Path("/path/to/lib.jar"))
        }
        val deserialized = serializer.serializeAndDeserialize(metadataArgs)
        assertEquals(metadataArgs.destination, deserialized.destination)
        assertEquals(metadataArgs.classpath, deserialized.classpath)
        assertEquals(metadataArgs.toArgumentStrings(), deserialized.toArgumentStrings())
    }

    @Test
    @DisplayName("Compiler plugin options and ordering are serialized correctly")
    fun compilerPluginsSerialization() {
        val plugin = CompilerPluginImpl(
            pluginId = "org.jetbrains.kotlin.allopen",
            classpath = listOf(Path("/plugins/allopen.jar")),
            rawArguments = listOf(CompilerPluginOptionImpl("annotation", "org.example.Open")),
            orderingRequirements = setOf(
                CompilerPluginPartialOrderImpl(CompilerPluginPartialOrderRelationImpl.BEFORE, "org.jetbrains.kotlin.noarg")
            )
        )
        val deserialized = serializer.serializeAndDeserialize(plugin)
        assertEquals(plugin.pluginId, deserialized.pluginId)
        assertEquals(plugin.classpath, deserialized.classpath)
        assertEquals(plugin.rawArguments.first().key, deserialized.rawArguments.first().key)
        assertEquals(plugin.rawArguments.first().value, deserialized.rawArguments.first().value)
        assertEquals(plugin.orderingRequirements.first().otherPluginId, deserialized.orderingRequirements.first().otherPluginId)
        assertEquals(plugin.orderingRequirements.first().relation, deserialized.orderingRequirements.first().relation)
    }

    @Test
    @DisplayName("Jsr305 sealed hierarchy serialization")
    fun jsr305Serialization() {
        val global = Jsr305Impl.Global(Jsr305Impl.Mode.STRICT)
        val migration = Jsr305Impl.UnderMigration(Jsr305Impl.Mode.WARN)
        val specific = Jsr305Impl.SpecificAnnotation("javax.annotation.Nonnull", Jsr305Impl.Mode.IGNORE)

        assertEquals(global, serializer.serializeAndDeserialize<Jsr305Impl.Global>(global))
        assertEquals(migration, serializer.serializeAndDeserialize<Jsr305Impl.UnderMigration>(migration))
        assertEquals(specific, serializer.serializeAndDeserialize<Jsr305Impl.SpecificAnnotation>(specific))
    }

    @Test
    @DisplayName("WarningLevel and NullabilityAnnotation serialization")
    fun warningLevelsAndNullabilitySerialization() {
        val warningLevel = WarningLevelImpl("UNUSED_VARIABLE", WarningLevelImpl.Severity.ERROR)
        val deserializedWarning = serializer.serializeAndDeserialize(warningLevel)
        assertEquals(warningLevel, deserializedWarning)

        val nullability = NullabilityAnnotationImpl("org.jspecify.nullness.Nullable", NullabilityAnnotationImpl.Mode.STRICT)
        val deserializedNullability = serializer.serializeAndDeserialize(nullability)
        assertEquals(nullability, deserializedNullability)
    }

    @Test
    @DisplayName("ProfileCompilerCommand and RestrictedArgViolation serialization")
    fun profileCompilerCommandAndRestrictedArgViolationSerialization() {
        val profileCmd = ProfileCompilerCommandImpl(Path("/path/to/profiler"), "start", Path("/path/to/out"))
        val deserializedCmd = serializer.serializeAndDeserialize(profileCmd)
        assertEquals(profileCmd, deserializedCmd)

        val warning = RestrictedArgViolation.Warning("warning message")
        val deserializedWarning = serializer.serializeAndDeserialize<RestrictedArgViolation.Warning>(warning)
        assertEquals(warning, deserializedWarning)

        val error = RestrictedArgViolation.Error("error message")
        val deserializedError = serializer.serializeAndDeserialize<RestrictedArgViolation.Error>(error)
        assertEquals(error, deserializedError)
    }

    @Test
    @DisplayName("DaemonExecutionPolicy default values and limits round-trip")
    fun daemonExecutionPolicyFullConfig() {
        val defaultDaemonPolicy = DaemonExecutionPolicyImpl()
        val deserializedDefaults = serializer.serializeAndDeserialize(defaultDaemonPolicy)
        assertEquals(defaultDaemonPolicy, deserializedDefaults)

        val fullDaemonPolicy = DaemonExecutionPolicyImpl().apply {
            logsPath = Path("/tmp/logs")
            daemonRunDirPath = Path("/tmp/daemonRun")
            jvmArguments = listOf("-Xmx2g", "-XX:+UseG1GC")
            shutdownDelayMillis = 5000L
            logsFileSizeLimit = 1024 * 1024L
            logsFileCountLimit = 5
        }
        val deserializedFull = serializer.serializeAndDeserialize(fullDaemonPolicy)
        assertEquals(fullDaemonPolicy, deserializedFull)
    }

    @Test
    @DisplayName("PathAsStringSerializer serializes paths correctly")
    fun pathAsStringSerializer() {
        val path = Path("/some/test/path")
        val bytes = ProtoBuf.encodeToByteArray(PathAsStringSerializer, path)
        val deserializedPath = ProtoBuf.decodeFromByteArray(PathAsStringSerializer, bytes)
        assertEquals(path, deserializedPath)
    }

    @Test
    @DisplayName("ExecutionPolicy classes are serialized correctly")
    fun executionPolicies() {
        val inProcess = InProcessExecutionPolicyImpl
        val inProcess2 = serializer.serializeAndDeserialize<InProcessExecutionPolicyImpl>(inProcess)
        assertEquals(inProcess, inProcess2)

        val withDaemon = DaemonExecutionPolicyImpl()
        withDaemon.logsPath = Path("/someBuildFile.xml")
        withDaemon.jvmArguments = listOf("abc")
        val withDaemon2 = serializer.serializeAndDeserialize<DaemonExecutionPolicyImpl>(withDaemon)
        assertEquals(withDaemon, withDaemon2)
    }

    @Test
    @DisplayName("KlibTargetId is serialized correctly")
    fun klibTargetId() {
        val targetId = KlibTargetId(KlibTargetType.IOS_ARM64, "iosArm64Custom")
        val byteArray = ProtoBuf.encodeToByteArray(KlibTargetIdSerializer, targetId)
        val deserialized = ProtoBuf.decodeFromByteArray(KlibTargetIdSerializer, byteArray)
        assertEquals(targetId, deserialized)
    }

    @Test
    @DisplayName("IcEventSerializer serializes IcEvent correctly")
    fun icEventsSerialization() {
        val startedEvent: IcEvent = CompilationStartedEvent(
            isIncremental = true,
            reason = "Some change",
            type = "CompilationStarted",
            timestamp = 123456789L,
            iteration = 2,
        )
        assertEquals(startedEvent, serializer.serializeAndDeserialize(startedEvent, IcEventSerializer))

        val compileIterationEvent: IcEvent = CompileIterationEvent(
            reasons = mapOf(
                "/path/to/File.kt" to listOf(FileCompilationReasonBasic("Reason 1"), FileCompilationReasonBasic("Reason 2")),
                "/path/to/Other.kt" to listOf(FileCompilationReasonBasic("Reason 3")),
            ),
            exitCode = "OK",
            type = "CompileIteration",
            timestamp = 987654321L,
            iteration = 2,
        )
        assertEquals(compileIterationEvent, serializer.serializeAndDeserialize(compileIterationEvent, IcEventSerializer))

        val customStarted: IcEvent.CompilationStarted = object : IcEvent.CompilationStarted {
            override val isIncremental = false
            override val reason = "Rebuild"
            override val type = "CompilationStarted"
            override val timestamp = 11111L
            override val iteration = 0
        }
        val deserializedCustom = serializer.serializeAndDeserialize(customStarted, IcEventSerializer) as CompilationStartedEvent
        assertEquals(customStarted.iteration, deserializedCustom.iteration)
        assertEquals(customStarted.timestamp, deserializedCustom.timestamp)
        assertEquals(customStarted.type, deserializedCustom.type)
        assertEquals(customStarted.isIncremental, deserializedCustom.isIncremental)
        assertEquals(customStarted.reason, deserializedCustom.reason)
    }

    @Test
    @DisplayName("Message.IcEventCollector.CollectEvents serialization")
    fun collectEventsMessageSerialization() {
        val startedEvent = CompilationStartedEvent(
            isIncremental = true,
            reason = "Some change",
            type = "CompilationStarted",
            timestamp = 123456789L,
            iteration = 2,
        )
        val compileIterationEvent = CompileIterationEvent(
            reasons = mapOf(
                "/path/to/File.kt" to listOf(FileCompilationReasonBasic("Reason 1"), FileCompilationReasonBasic("Reason 2")),
                "/path/to/Other.kt" to listOf(FileCompilationReasonBasic("Reason 3")),
            ),
            exitCode = "OK",
            type = "CompileIteration",
            timestamp = 987654321L,
            iteration = 2,
        )
        val message = Message.IcEventCollector.CollectEvents(listOf(startedEvent, compileIterationEvent))

        val deserialized = serializer.serializeAndDeserialize(message)
        assertEquals(message, deserialized)

        val deserializedAsMessage = serializer.serializeAndDeserialize<Message>(message)
        assertEquals(message, deserializedAsMessage)

        val emptyMessage = Message.IcEventCollector.CollectEvents(emptyList())
        val deserializedEmpty = serializer.serializeAndDeserialize<Message>(emptyMessage)
        assertEquals(emptyMessage, deserializedEmpty)
    }

    @Test
    @DisplayName("JVM build operations are serialized correctly")
    fun jvmOperationsSerialization() {
        val jvmCompilation = JvmCompilationOperationImpl(
            listOf(Path("/src/Main.kt")),
            Path("/out/classes"),
            JvmCompilerArgumentsImpl().apply { jdkHome = Path("/jdk") },
            "2.2.0"
        )
        val deserializedJvmComp = serializer.serializeAndDeserialize(jvmCompilation)
        assertEquals(jvmCompilation.sources, deserializedJvmComp.sources)
        assertEquals(jvmCompilation.destinationDirectory, deserializedJvmComp.destinationDirectory)
        assertEquals(jvmCompilation.compilerArguments.jdkHome, deserializedJvmComp.compilerArguments.jdkHome)
    }

    @Test
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    @DisplayName("Incremental compilation configurations are serialized correctly")
    fun incrementalCompilationConfigurationsSerialization() {
        val jvmSnapshotIc = JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            Path("/working/dir"),
            SourcesChanges.Known(listOf(File("/src/A.kt")), emptyList()),
            listOf(Path("/dep/snapshot.bin")),
            Path("/shrunk/snapshot.bin")
        ).apply {
            preciseJavaTracking = true
            assuredNoClasspathSnapshotChanges = true
            useFirRunner = true
            rootProjectDir = Path("/root")
            moduleBuildDir = Path("/build")
        }
        val deserializedJvmSnapshotIc = serializer.serializeAndDeserialize<JvmIncrementalCompilationConfiguration>(
            jvmSnapshotIc
        ) as JvmSnapshotBasedIncrementalCompilationConfigurationImpl
        val deserializedJvmSources = deserializedJvmSnapshotIc.sourcesChanges as SourcesChanges.Known
        assertEquals(jvmSnapshotIc.workingDirectory, deserializedJvmSnapshotIc.workingDirectory)
        assertEquals(listOf(File("/src/A.kt")), deserializedJvmSources.modifiedFiles)
        assertEquals(jvmSnapshotIc.dependenciesSnapshotFiles, deserializedJvmSnapshotIc.dependenciesSnapshotFiles)
        assertEquals(jvmSnapshotIc.shrunkClasspathSnapshot, deserializedJvmSnapshotIc.shrunkClasspathSnapshot)
        assertEquals(jvmSnapshotIc.preciseJavaTracking, deserializedJvmSnapshotIc.preciseJavaTracking)
        assertEquals(jvmSnapshotIc.assuredNoClasspathSnapshotChanges, deserializedJvmSnapshotIc.assuredNoClasspathSnapshotChanges)
        assertEquals(jvmSnapshotIc.useFirRunner, deserializedJvmSnapshotIc.useFirRunner)
        assertEquals(jvmSnapshotIc.rootProjectDir, deserializedJvmSnapshotIc.rootProjectDir)
        assertEquals(jvmSnapshotIc.moduleBuildDir, deserializedJvmSnapshotIc.moduleBuildDir)
    }
}
