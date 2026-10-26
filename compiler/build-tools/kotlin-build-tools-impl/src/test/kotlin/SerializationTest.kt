/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class, ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.InaccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.AccessibleClassSnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.ClasspathEntrySnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl
import org.jetbrains.kotlin.buildtools.internal.InProcessExecutionPolicyImpl
import org.jetbrains.kotlin.buildtools.internal.LogLevel
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.*
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsDtsGenerationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsLinkingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.DiscoverScriptExtensionsOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.operations.KotlinMetadataKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.*
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmLinkingOperationImpl
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.incremental.classpathDiff.JavaClassSnapshot
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {

    interface Serializer {
        fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T

        object Protobuf : Serializer {
            private val protoBuf = ProtoBuf { serializersModule = btaSerializersModule }

            override fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T {
                val ser = protoBuf.serializersModule.serializer(clazz.java)
                val byteArray = protoBuf.encodeToByteArray(ser, value)
                @Suppress("UNCHECKED_CAST")
                return protoBuf.decodeFromByteArray(ser, byteArray) as T
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

    @Test
    @DisplayName("KlibTargetId is serialized correctly")
    fun klibTargetId() {
        val targetId = KlibTargetId(KlibTargetType.IOS_ARM64, "iosArm64Custom")
        val byteArray = ProtoBuf.encodeToByteArray(KlibTargetIdSerializer, targetId)
        val deserialized = ProtoBuf.decodeFromByteArray(KlibTargetIdSerializer, byteArray)
        assertEquals(targetId, deserialized)
    }

    @Test
    @DisplayName("ClasspathEntrySnapshot is serialized correctly")
    fun classpathEntrySnapshot() {
        val origin = org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot(
            linkedMapOf(
                "com/example/A.class" to JavaClassSnapshot(
                    ClassId(FqName("com.example"), FqName("A"), false),
                    42L,
                    null,
                    emptyList()
                ),
                "com/example/B.class" to org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot,
            )
        )
        val snapshot: ClasspathEntrySnapshot = ClasspathEntrySnapshotImpl(origin)

        val byteArray = ProtoBuf.encodeToByteArray(ClasspathEntrySnapshotSerializer, snapshot)
        val deserialized = ProtoBuf.decodeFromByteArray(ClasspathEntrySnapshotSerializer, byteArray)

        assertEquals(snapshot, deserialized)
        val classSnapshots = deserialized.classSnapshots
        assertEquals(2, classSnapshots.size)
        val classA = classSnapshots["com/example/A.class"]
        val classB = classSnapshots["com/example/B.class"]
        assertEquals(AccessibleClassSnapshotImpl(42L), classA)
        assertEquals(42L, (classA as AccessibleClassSnapshot).classAbiHash)
        assertEquals(true, classB is InaccessibleClassSnapshot)
    }

    @Test
    @DisplayName("ABI filters are serialized correctly")
    fun abiFiltersSerialization() {
        val abiFilters = AbiFiltersImpl().apply {
            includeNamed = setOf("com.example.*")
            excludeNamed = setOf("com.example.internal.*")
            includeAnnotatedWith = setOf("com.example.MyAnnotation")
            excludeAnnotatedWith = setOf("com.example.InternalAnnotation")
        }
        val deserializedFilters = serializer.serializeAndDeserialize(abiFilters)
        assertEquals(abiFilters.includeNamed, deserializedFilters.includeNamed)
        assertEquals(abiFilters.excludeNamed, deserializedFilters.excludeNamed)
        assertEquals(abiFilters.includeAnnotatedWith, deserializedFilters.includeAnnotatedWith)
        assertEquals(abiFilters.excludeAnnotatedWith, deserializedFilters.excludeAnnotatedWith)
    }

    @Test
    @DisplayName("JVM build operations are serialized correctly")
    fun jvmOperationsSerialization() {
        val discoverScript = DiscoverScriptExtensionsOperationImpl(listOf(Path("/some/script.jar")))
        val deserializedDiscover = serializer.serializeAndDeserialize(discoverScript)
        assertEquals(discoverScript.classpath, deserializedDiscover.classpath)

        val classpathSnapshotting = JvmClasspathSnapshottingOperationImpl(Path("/some/entry.jar")).apply {
            granularity = ClassSnapshotGranularity.CLASS_LEVEL
            parseInlinedLocalClasses = false
            expandTypeAliases = true
        }
        val deserializedSnapshotting = serializer.serializeAndDeserialize(classpathSnapshotting)
        assertEquals(classpathSnapshotting.classpathEntry, deserializedSnapshotting.classpathEntry)
        assertEquals(classpathSnapshotting.granularity, deserializedSnapshotting.granularity)
        assertEquals(classpathSnapshotting.parseInlinedLocalClasses, deserializedSnapshotting.parseInlinedLocalClasses)
        assertEquals(classpathSnapshotting.expandTypeAliases, deserializedSnapshotting.expandTypeAliases)

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
    @DisplayName("JS, Wasm, and Metadata operations are serialized correctly")
    fun jsWasmMetadataOperationsSerialization() {
        val jsDts = JsDtsGenerationOperationImpl(
            listOf(Path("/klib/app.klib")),
            Path("/out/dts"),
            tsCompilationStrategy = JsDtsCompilationStrategy.MERGED,
            granularity = JsDtsGranularity.PER_FILE,
            moduleKind = JsModuleKind.ES,
            compileLongAsBigInt = true,
            implementInterfaces = true,
            exportSuspendLambdas = true,
            exportUntypedAsUnknown = true,
            dataClassCopyRespectsConstructorVisibility = true,
        )
        val deserializedJsDts = serializer.serializeAndDeserialize(jsDts)
        assertEquals(jsDts.klibs, deserializedJsDts.klibs)
        assertEquals(jsDts.outputDirectory, deserializedJsDts.outputDirectory)
        assertEquals(jsDts.tsCompilationStrategy, deserializedJsDts.tsCompilationStrategy)
        assertEquals(jsDts.granularity, deserializedJsDts.granularity)
        assertEquals(jsDts.moduleKind, deserializedJsDts.moduleKind)
        assertEquals(jsDts.compileLongAsBigInt, deserializedJsDts.compileLongAsBigInt)
        assertEquals(jsDts.implementInterfaces, deserializedJsDts.implementInterfaces)
        assertEquals(jsDts.exportSuspendLambdas, deserializedJsDts.exportSuspendLambdas)
        assertEquals(jsDts.exportUntypedAsUnknown, deserializedJsDts.exportUntypedAsUnknown)
        assertEquals(jsDts.dataClassCopyRespectsConstructorVisibility, deserializedJsDts.dataClassCopyRespectsConstructorVisibility)

        val jsKlib = JsKlibCompilationOperationImpl(
            listOf(Path("/src/Main.kt")),
            Path("/out/klib"),
            JsArgumentsImpl().apply { sourceMap = true },
            "2.2.0"
        )
        val deserializedJsKlib = serializer.serializeAndDeserialize(jsKlib)
        assertEquals(jsKlib.sources, deserializedJsKlib.sources)
        assertEquals(jsKlib.destination, deserializedJsKlib.destination)
        assertEquals(jsKlib.compilerArguments.sourceMap, deserializedJsKlib.compilerArguments.sourceMap)

        val jsLinking = JsLinkingOperationImpl(
            Path("/klib/app.klib"),
            Path("/out/js"),
            JsArgumentsImpl().apply { moduleKind = JsModuleKind.COMMONJS }
        )
        val deserializedJsLinking = serializer.serializeAndDeserialize(jsLinking)
        assertEquals(jsLinking.klib, deserializedJsLinking.klib)
        assertEquals(jsLinking.destination, deserializedJsLinking.destination)
        assertEquals(jsLinking.compilerArguments.moduleKind, deserializedJsLinking.compilerArguments.moduleKind)

        val wasmKlib = WasmKlibCompilationOperationImpl(
            listOf(Path("/src/Main.kt")),
            Path("/out/klib"),
            WasmArgumentsImpl().apply { wasmTarget = WasmTarget.WASM_WASI },
            "2.2.0"
        )
        val deserializedWasmKlib = serializer.serializeAndDeserialize(wasmKlib)
        assertEquals(wasmKlib.sources, deserializedWasmKlib.sources)
        assertEquals(wasmKlib.destination, deserializedWasmKlib.destination)
        assertEquals(wasmKlib.compilerArguments.wasmTarget, deserializedWasmKlib.compilerArguments.wasmTarget)

        val wasmLinking = WasmLinkingOperationImpl(
            Path("/klib/app.klib"),
            Path("/out/wasm"),
            WasmArgumentsImpl()
        )
        val deserializedWasmLinking = serializer.serializeAndDeserialize(wasmLinking)
        assertEquals(wasmLinking.klib, deserializedWasmLinking.klib)
        assertEquals(wasmLinking.destination, deserializedWasmLinking.destination)

        val metadataKlib = KotlinMetadataKlibCompilationOperationImpl(
            listOf(Path("/src/Main.kt")),
            Path("/out/klib"),
            MetadataArgumentsImpl().apply { destination = "/dest" },
            "2.2.0"
        )
        val deserializedMetadataKlib = serializer.serializeAndDeserialize(metadataKlib)
        assertEquals(metadataKlib.sources, deserializedMetadataKlib.sources)
        assertEquals(metadataKlib.destination, deserializedMetadataKlib.destination)
        assertEquals(metadataKlib.compilerArguments.destination, deserializedMetadataKlib.compilerArguments.destination)
    }

    @Test
    @DisplayName("Incremental compilation configurations are serialized correctly")
    fun incrementalCompilationConfigurationsSerialization() {
        val jsIncrementalModule = org.jetbrains.kotlin.buildtools.api.js.IncrementalModule(
            "modJs",
            Path("/path/to/out"),
            Path("/path/to/build"),
            Path("/path/to/history")
        )
        val jsSourcesChanges = SourcesChanges.Known(listOf(File("/src/A.kt")), listOf(File("/src/B.kt")))
        val jsIc = JsHistoryBasedIncrementalCompilationConfigurationImpl(
            Path("/working/dir"),
            jsSourcesChanges,
            listOf(jsIncrementalModule)
        ).apply {
            rootProjectDir = Path("/root")
            moduleBuildDir = Path("/build")
            backupClasses = true
            keepIcCachesInMemory = true
            forceRecompilation = true
            outputDirs = setOf(Path("/out1"), Path("/out2"))
            rootProjectBuildDir = Path("/rootBuild")
            historyFileDir = Path("/historyDir")
        }
        val deserializedJsIc = serializer.serializeAndDeserialize(jsIc)
        val deserializedJsSources = deserializedJsIc.sourcesChanges as SourcesChanges.Known
        assertEquals(jsIc.workingDirectory, deserializedJsIc.workingDirectory)
        assertEquals(jsSourcesChanges.modifiedFiles, deserializedJsSources.modifiedFiles)
        assertEquals(jsSourcesChanges.removedFiles, deserializedJsSources.removedFiles)
        assertEquals(jsIc.modulesInformation.size, deserializedJsIc.modulesInformation.size)
        assertEquals(jsIc.modulesInformation.first().name, deserializedJsIc.modulesInformation.first().name)
        assertEquals(jsIc.modulesInformation.first().output, deserializedJsIc.modulesInformation.first().output)
        assertEquals(jsIc.modulesInformation.first().buildDir, deserializedJsIc.modulesInformation.first().buildDir)
        assertEquals(jsIc.modulesInformation.first().buildHistoryDir, deserializedJsIc.modulesInformation.first().buildHistoryDir)
        assertEquals(jsIc.rootProjectDir, deserializedJsIc.rootProjectDir)
        assertEquals(jsIc.moduleBuildDir, deserializedJsIc.moduleBuildDir)
        assertEquals(jsIc.backupClasses, deserializedJsIc.backupClasses)
        assertEquals(jsIc.keepIcCachesInMemory, deserializedJsIc.keepIcCachesInMemory)
        assertEquals(jsIc.forceRecompilation, deserializedJsIc.forceRecompilation)
        assertEquals(jsIc.outputDirs, deserializedJsIc.outputDirs)
        assertEquals(jsIc.rootProjectBuildDir, deserializedJsIc.rootProjectBuildDir)
        assertEquals(jsIc.historyFileDir, deserializedJsIc.historyFileDir)

        val wasmIncrementalModule = org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule(
            "modWasm",
            Path("/path/to/out"),
            Path("/path/to/build"),
            Path("/path/to/history")
        )
        val wasmSourcesChanges = SourcesChanges.Known(listOf(File("/src/A.kt")), listOf(File("/src/B.kt")))
        val wasmIc = WasmHistoryBasedIncrementalCompilationConfigurationImpl(
            Path("/working/dir"),
            wasmSourcesChanges,
            listOf(wasmIncrementalModule)
        ).apply {
            rootProjectDir = Path("/root")
            moduleBuildDir = Path("/build")
            rootProjectBuildDir = Path("/rootBuild")
            historyFileDir = Path("/historyDir")
        }
        val deserializedWasmIc = serializer.serializeAndDeserialize(wasmIc)
        val deserializedWasmSources = deserializedWasmIc.sourcesChanges as SourcesChanges.Known
        assertEquals(wasmIc.workingDirectory, deserializedWasmIc.workingDirectory)
        assertEquals(wasmSourcesChanges.modifiedFiles, deserializedWasmSources.modifiedFiles)
        assertEquals(wasmSourcesChanges.removedFiles, deserializedWasmSources.removedFiles)
        assertEquals(wasmIc.modulesInformation.first().name, deserializedWasmIc.modulesInformation.first().name)
        assertEquals(wasmIc.modulesInformation.first().output, deserializedWasmIc.modulesInformation.first().output)
        assertEquals(wasmIc.modulesInformation.first().buildDir, deserializedWasmIc.modulesInformation.first().buildDir)
        assertEquals(wasmIc.modulesInformation.first().buildHistoryDir, deserializedWasmIc.modulesInformation.first().buildHistoryDir)
        assertEquals(wasmIc.rootProjectDir, deserializedWasmIc.rootProjectDir)
        assertEquals(wasmIc.moduleBuildDir, deserializedWasmIc.moduleBuildDir)
        assertEquals(wasmIc.rootProjectBuildDir, deserializedWasmIc.rootProjectBuildDir)
        assertEquals(wasmIc.historyFileDir, deserializedWasmIc.historyFileDir)

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
        val jvmSnapshotBytes = ProtoBuf { serializersModule = btaSerializersModule }.encodeToByteArray(
            JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer,
            jvmSnapshotIc
        )
        val deserializedJvmSnapshotIc = ProtoBuf { serializersModule = btaSerializersModule }.decodeFromByteArray(
            JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer,
            jvmSnapshotBytes
        )
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

    @Test
    @DisplayName("Message hierarchy subclasses are serialized correctly")
    fun messagesSerialization() {
        val collectMetric = Message.CollectMetricMessage("buildTime", BuildMetricsCollector.ValueType.NANOSECONDS, 100L)
        assertEquals(collectMetric, serializer.serializeAndDeserialize<Message.CollectMetricMessage>(collectMetric))

        val lookupRecord = Message.LookupTracker.Record("path/A.kt", "com.example.A", CompilerLookupTracker.ScopeKind.CLASSIFIER, "foo")
        assertEquals(lookupRecord, serializer.serializeAndDeserialize<Message.LookupTracker.Record>(lookupRecord))

        val lookupClear = Message.LookupTracker.Clear
        assertEquals(lookupClear, serializer.serializeAndDeserialize<Message.LookupTracker.Clear>(lookupClear))

        val loc = CompilerMessageLocation.create("/src/A.kt", 10, 5, "val a = ")
        val compilerMsgReport = Message.CompilerMessage.Report(CompilerMessageSeverity.ERROR, "Syntax error", loc, "DIAG_1")
        assertEquals(compilerMsgReport, serializer.serializeAndDeserialize<Message.CompilerMessage.Report>(compilerMsgReport))

        val compilerMsgClear = Message.CompilerMessage.Clear
        assertEquals(compilerMsgClear, serializer.serializeAndDeserialize<Message.CompilerMessage.Clear>(compilerMsgClear))

        val logLine = Message.Logger.LogLine(LogLevel.INFO, "Compiling...")
        assertEquals(logLine, serializer.serializeAndDeserialize<Message.Logger.LogLine>(logLine))

        val mapOut = Message.FileMappingTracker.RecordSourceFilesToOutputFileMapping(listOf(Path("/src/A.kt")), Path("/out/A.class"))
        assertEquals(mapOut, serializer.serializeAndDeserialize<Message.FileMappingTracker.RecordSourceFilesToOutputFileMapping>(mapOut))

        val mapRef = Message.FileMappingTracker.RecordSourceReferencedByCompilerPlugin(Path("/src/A.kt"))
        assertEquals(mapRef, serializer.serializeAndDeserialize<Message.FileMappingTracker.RecordSourceReferencedByCompilerPlugin>(mapRef))

        val mapGenOut = Message.FileMappingTracker.RecordOutputFileGeneratedForPlugin(Path("/out/Plugin.class"))
        assertEquals(mapGenOut, serializer.serializeAndDeserialize<Message.FileMappingTracker.RecordOutputFileGeneratedForPlugin>(mapGenOut))

        val mapGenSrc = Message.FileMappingTracker.RecordSourceFileGeneratedForPlugin(Path("/src/Generated.kt"))
        assertEquals(mapGenSrc, serializer.serializeAndDeserialize<Message.FileMappingTracker.RecordSourceFileGeneratedForPlugin>(mapGenSrc))

        val enumWhen = Message.EnumWhenTracker.Report(Path("/src/A.kt"), "com/example/MyEnum")
        assertEquals(enumWhen, serializer.serializeAndDeserialize<Message.EnumWhenTracker.Report>(enumWhen))

        val importRep = Message.ImportTracker.Report(Path("/src/A.kt"), "com.example.package.*")
        assertEquals(importRep, serializer.serializeAndDeserialize<Message.ImportTracker.Report>(importRep))

        val inlineConst = Message.InlineConstTracker.Report(Path("/src/A.kt"), "com/example/ConstOwner", "MY_CONST", "I")
        assertEquals(inlineConst, serializer.serializeAndDeserialize<Message.InlineConstTracker.Report>(inlineConst))

        val appendMsg = Message.Appendable.Append("some appended text")
        assertEquals(appendMsg, serializer.serializeAndDeserialize<Message.Appendable.Append>(appendMsg))
    }

    @Test
    @DisplayName("SourcesChanges, CompilerMessageSourceLocation, and CompilationResult serializers work correctly")
    fun customSerializersRoundTrip() {
        val known: SourcesChanges = SourcesChanges.Known(listOf(File("/src/A.kt")), listOf(File("/src/B.kt")))
        val knownBytes = ProtoBuf.encodeToByteArray(SourcesChangesSerializer, known)
        val deserializedKnown = ProtoBuf.decodeFromByteArray(SourcesChangesSerializer, knownBytes)
        assertEquals((known as SourcesChanges.Known).modifiedFiles, (deserializedKnown as SourcesChanges.Known).modifiedFiles)
        assertEquals(known.removedFiles, deserializedKnown.removedFiles)

        val unknown: SourcesChanges = SourcesChanges.Unknown
        val unknownBytes = ProtoBuf.encodeToByteArray(SourcesChangesSerializer, unknown)
        val deserializedUnknown = ProtoBuf.decodeFromByteArray(SourcesChangesSerializer, unknownBytes)
        assertEquals(unknown, deserializedUnknown)

        val toBeCalculated: SourcesChanges = SourcesChanges.ToBeCalculated
        val toBeCalculatedBytes = ProtoBuf.encodeToByteArray(SourcesChangesSerializer, toBeCalculated)
        val deserializedToBeCalculated = ProtoBuf.decodeFromByteArray(SourcesChangesSerializer, toBeCalculatedBytes)
        assertEquals(toBeCalculated, deserializedToBeCalculated)

        val loc = CompilerMessageLocation.create("/src/Main.kt", 10, 5, "val x = 1")!!
        val locBytes = ProtoBuf.encodeToByteArray(CompilerMessageSourceLocationSerializer, loc)
        val deserializedLoc = ProtoBuf.decodeFromByteArray(CompilerMessageSourceLocationSerializer, locBytes)
        assertEquals(loc.path, deserializedLoc.path)
        assertEquals(loc.line, deserializedLoc.line)
        assertEquals(loc.column, deserializedLoc.column)
        assertEquals(loc.lineContent, deserializedLoc.lineContent)

        val locRange = CompilerMessageLocationWithRange.create("/src/Main.kt", 10, 5, 10, 20, "val x = 1")!!
        val locRangeBytes = ProtoBuf.encodeToByteArray(CompilerMessageSourceLocationSerializer, locRange)
        val deserializedLocRange = ProtoBuf.decodeFromByteArray(CompilerMessageSourceLocationSerializer, locRangeBytes)
        assertEquals(locRange.path, deserializedLocRange.path)
        assertEquals(locRange.line, deserializedLocRange.line)
        assertEquals(locRange.column, deserializedLocRange.column)
        assertEquals(locRange.lineEnd, deserializedLocRange.lineEnd)
        assertEquals(locRange.columnEnd, deserializedLocRange.columnEnd)
        assertEquals(locRange.lineContent, deserializedLocRange.lineContent)

        val jsMod = org.jetbrains.kotlin.buildtools.api.js.IncrementalModule("jsMod", Path("/out"), Path("/build"), Path("/history"))
        val jsModBytes = ProtoBuf.encodeToByteArray(JsIncrementalModuleSerializer, jsMod)
        val deserializedJsMod = ProtoBuf.decodeFromByteArray(JsIncrementalModuleSerializer, jsModBytes)
        assertEquals(jsMod.name, deserializedJsMod.name)
        assertEquals(jsMod.output, deserializedJsMod.output)
        assertEquals(jsMod.buildDir, deserializedJsMod.buildDir)
        assertEquals(jsMod.buildHistoryDir, deserializedJsMod.buildHistoryDir)

        val wasmMod = org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule("wasmMod", Path("/out"), Path("/build"), Path("/history"))
        val wasmModBytes = ProtoBuf.encodeToByteArray(WasmIncrementalModuleSerializer, wasmMod)
        val deserializedWasmMod = ProtoBuf.decodeFromByteArray(WasmIncrementalModuleSerializer, wasmModBytes)
        assertEquals(wasmMod.name, deserializedWasmMod.name)
        assertEquals(wasmMod.output, deserializedWasmMod.output)
        assertEquals(wasmMod.buildDir, deserializedWasmMod.buildDir)
        assertEquals(wasmMod.buildHistoryDir, deserializedWasmMod.buildHistoryDir)

        val successRes = CompilationResult.COMPILATION_SUCCESS
        val successBytes = ProtoBuf.encodeToByteArray(CompilationResultSerializer, successRes)
        val deserializedSuccess = ProtoBuf.decodeFromByteArray(CompilationResultSerializer, successBytes)
        assertEquals(successRes, deserializedSuccess)

        val errorRes = CompilationResult.COMPILATION_ERROR
        val errorBytes = ProtoBuf.encodeToByteArray(CompilationResultSerializer, errorRes)
        val deserializedError = ProtoBuf.decodeFromByteArray(CompilationResultSerializer, errorBytes)
        assertEquals(errorRes, deserializedError)
    }
}
