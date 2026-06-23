/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.jvm.KaptCompilerPlugin
import org.jetbrains.kotlin.buildtools.api.jvm.KaptDetectMemoryLeaksMode
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import org.jetbrains.kotlin.buildtools.internal.jvm.KaptCompilerPluginImpl.AptPhaseImpl
import java.nio.file.Path

internal class KaptCompilerPluginImpl(
    val options: Options = Options(KaptCompilerPlugin::class),
    override val kaptClasspath: List<Path>,
    override var aptPhase: KaptCompilerPlugin.AptPhase? = null,
    override var stubsPhase: KaptCompilerPlugin.StubsPhase? = null,
) :
    KaptCompilerPlugin, KaptCompilerPlugin.Builder, DeepCopyable<KaptCompilerPluginImpl> {

    constructor(kaptClasspath: List<Path>) : this(Options(KaptCompilerPlugin::class), kaptClasspath = kaptClasspath.toList()) {
        initializeOptions(this::class, options)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptCompilerPlugin.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptCompilerPlugin.Option<V>, value: V) {
        options[key] = value
    }

    override fun aptPhaseBuilder(): KaptCompilerPlugin.AptPhase.Builder = AptPhaseImpl()

    override fun stubsPhaseBuilder(): KaptCompilerPlugin.StubsPhase.Builder = StubsPhaseImpl()

    override fun toBuilder(): KaptCompilerPlugin.Builder = deepCopy()
    override fun toCompilerPlugin(): CompilerPlugin {
        return CompilerPlugin(
            PLUGIN_ID,
            classpath = kaptClasspath,
            rawArguments = toCompilerPluginOptions(),
            orderingRequirements = emptySet(),
        )
    }

    private fun toCompilerPluginOptions(): List<CompilerPluginOption> {
        val compilerPluginOptions = mutableListOf<CompilerPluginOption>()
        val aptMode = when {
            aptPhase != null && stubsPhase != null -> "stubsAndApt"
            aptPhase != null -> "apt"
            stubsPhase != null -> "stubs"
            else -> error("Apt or stubs phase configuration required")
        }
        compilerPluginOptions.add(CompilerPluginOption("aptMode", aptMode))
        options.iterator().toCompilerPluginOptions(compilerPluginOptions)
        (aptPhase as? AptPhaseImpl)?.options?.iterator()?.toCompilerPluginOptions(compilerPluginOptions)
        (stubsPhase as? StubsPhaseImpl)?.options?.iterator()?.toCompilerPluginOptions(compilerPluginOptions)

        return compilerPluginOptions
    }

    fun Iterator<Map.Entry<String, Any?>>.toCompilerPluginOptions(compilerPluginOptions: MutableList<CompilerPluginOption> = mutableListOf()): List<CompilerPluginOption> {
        this.forEach { (key, value) ->
            value?.toCompilerPluginOptions(key, compilerPluginOptions)
        }
        return compilerPluginOptions
    }

    fun Any.toCompilerPluginOptions(
        outerKey: String,
        compilerPluginOptions: MutableList<CompilerPluginOption> = mutableListOf(),
    ): List<CompilerPluginOption> {
        when (this) {
            is Path -> compilerPluginOptions.add(
                CompilerPluginOption(
                    outerKey, this.absolutePathStringOrThrow()
                )
            )
            is Collection<*> -> this.forEach {
                it?.toCompilerPluginOptions(outerKey, compilerPluginOptions)
            }
            is Map<*, *> -> this.forEach { (key, value) ->
                require(key is String && value is String) { "Only Strings supported in key and values for map arguments for now" }
                val keyValue = listOf(key, value).filterNot { it.isNotEmpty() }.joinToString("=")
                compilerPluginOptions.add(CompilerPluginOption(outerKey, keyValue))
            }
            else -> compilerPluginOptions.add(CompilerPluginOption(outerKey, this.toString()))
        }
        return compilerPluginOptions
    }

    override fun build(): KaptCompilerPlugin = deepCopy()

    override fun deepCopy(): KaptCompilerPluginImpl {
        return KaptCompilerPluginImpl(
            options.deepCopy(),
            kaptClasspath,
            aptPhase?.toBuilder()?.build(),
            stubsPhase?.toBuilder()?.build()
        )
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        private const val PLUGIN_ID: String = "org.jetbrains.kotlin.kapt3"

        val INCREMENTAL_DATA_OUTPUT_DIR: Option<Path?> = Option("incrementalData", null)
        val TOOLS_JAR: Option<Path?> = Option("toolsJarLocation", null)
        val DUMP_DEFAULT_PARAMETER_VALUES: Option<Boolean> = Option("dumpDefaultParameterValues", false)
        val VERBOSE: Option<Boolean> = Option("verbose", false)
        val INFO_AS_WARNINGS: Option<Boolean> = Option("infoAsWarnings", false)
    }

    internal class AptPhaseImpl(val options: Options = Options(KaptCompilerPlugin.AptPhase::class)) : KaptCompilerPlugin.AptPhase,
        KaptCompilerPlugin.AptPhase.Builder, DeepCopyable<AptPhaseImpl> {

        constructor() : this(Options(KaptCompilerPlugin.AptPhase::class)) {
            initializeOptions(this::class, options)
        }

        @UseFromImplModuleRestricted
        override fun <V> get(key: KaptCompilerPlugin.AptPhase.Option<V>): V {
            return options[key]
        }

        @UseFromImplModuleRestricted
        override fun <V> set(key: KaptCompilerPlugin.AptPhase.Option<V>, value: V) {
            options[key] = value
        }

        override fun deepCopy(): AptPhaseImpl = AptPhaseImpl(options.deepCopy())

        override fun build(): KaptCompilerPlugin.AptPhase = deepCopy()

        override fun toBuilder(): KaptCompilerPlugin.AptPhase.Builder = deepCopy()

        class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

        companion object {
            val INCLUDE_COMPILE_CLASSPATH: Option<Boolean> = Option("includeCompileClasspath", true)

            val CORRECT_ERROR_TYPES: Option<Boolean> = Option("correctErrorTypes", false)

            val SOURCE_OUTPUT_DIR: Option<Path?> = Option("sources", null)

            val CLASS_OUTPUT_DIR: Option<Path?> = Option("classes", null)

            val CHANGED_FILES: Option<List<Path>?> = Option("changedFile", null)

            val COMPILED_SOURCES_DIR: Option<List<Path>?> = Option("compiledSourcesDir", null)

            val INCREMENTAL_CACHE: Option<Path?> = Option("incrementalCache", null)

            val CLASSPATH_CHANGES: Option<List<String>?> = Option("classpathChange", null)

            val PROCESS_INCREMENTALLY: Option<Boolean> = Option("processIncrementally", false)

            val ANNOTATION_PROCESSOR_CLASSPATH: Option<List<Path>?> = Option("apclasspath", null)

            val ANNOTATION_PROCESSORS: Option<List<String>?> = Option("processors", null)

            val APT_OPTIONS: Option<Map<String, String>?> = Option("apOption", null)

            val JAVAC_OPTIONS: Option<Map<String, String>?> = Option("javacOption", null)

            val DETECT_MEMORY_LEAKS: Option<KaptDetectMemoryLeaksMode> = Option("detectMemoryLeaks", KaptDetectMemoryLeaksMode.NONE)

            val DUMP_FILE_READ_HISTORY: Option<Path?> = Option("dumpFileReadHistory", null)

            val SHOW_PROCESSOR_STATS: Option<Boolean> = Option("showProcessorStats", false)

            val DUMP_PROCESSOR_STATS: Option<Path?> = Option("dumpProcessorStats", null)
        }
    }

    internal class StubsPhaseImpl(val options: Options = Options(KaptCompilerPlugin.StubsPhase::class)) : KaptCompilerPlugin.StubsPhase,
        KaptCompilerPlugin.StubsPhase.Builder, DeepCopyable<StubsPhaseImpl> {

        constructor() : this(Options(KaptCompilerPlugin.AptPhase::class)) {
            initializeOptions(this::class, options)
        }

        @UseFromImplModuleRestricted
        override fun <V> get(key: KaptCompilerPlugin.StubsPhase.Option<V>): V {
            return options[key]
        }

        @UseFromImplModuleRestricted
        override fun <V> set(key: KaptCompilerPlugin.StubsPhase.Option<V>, value: V) {
            options[key] = value
        }

        override fun deepCopy(): StubsPhaseImpl = StubsPhaseImpl(options.deepCopy())

        override fun build(): KaptCompilerPlugin.StubsPhase = deepCopy()

        override fun toBuilder(): KaptCompilerPlugin.StubsPhase.Builder = deepCopy()

        class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

        companion object {
            val STUBS_OUTPUT_DIR: Option<Path?> = Option("stubs", null)

            val STRIP_METADATA: Option<Boolean> = Option("stripMetadata", false)

            val STRICT_MODE: Option<Boolean> = Option("strict", false)

            val MAP_DIAGNOSTIC_LOCATIONS: Option<Boolean> = Option("mapDiagnosticLocations", false)
        }
    }
}

public fun main() {
    val a = KaptCompilerPluginImpl(emptyList())
    a.stubsPhase = a.stubsPhaseBuilder().build()
    a.aptPhase = a.aptPhaseBuilder().build()
    println(a.toCompilerPlugin())
}
