@file:OptIn(ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl
import org.jetbrains.kotlin.buildtools.internal.InProcessExecutionPolicyImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.*
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SerializationTest {

    interface Serializer {
        fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T

        object Protobuf : Serializer {
            override fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T {
                val byteArray = ProtoBuf.encodeToByteArray(serializer(clazz.java), value)
                @Suppress("UNCHECKED_CAST")
                return ProtoBuf.decodeFromByteArray(serializer(clazz.java), byteArray) as T
            }
        }
    }

    inline fun <reified T : Any> Serializer.serializeAndDeserialize(value: T): T = serializeAndDeserialize(value, T::class)

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
}
