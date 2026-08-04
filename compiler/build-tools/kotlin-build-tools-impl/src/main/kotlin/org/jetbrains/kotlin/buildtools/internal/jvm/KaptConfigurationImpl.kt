/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.jvm.KaptConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.KaptDetectMemoryLeaksMode
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

internal class KaptConfigurationImpl(
    val options: Options = Options(KaptConfiguration::class),
    override val kaptClasspath: List<Path>,
    override val stubsOutputDir: Path,
    override val sourcesOutputDir: Path,
    override val annotationProcessorsClasspath: List<Path>,
    var hasAptPhase: Boolean = false,
    var hasStubsPhase: Boolean = false,
) : KaptConfiguration, KaptConfiguration.Builder, KaptConfiguration.AptPhase, KaptConfiguration.AptPhase.Builder,
    KaptConfiguration.StubsPhase, KaptConfiguration.StubsPhase.Builder, DeepCopyable<KaptConfigurationImpl> {

    override val stubsPhase: KaptConfiguration.StubsPhase? get() = if (hasStubsPhase) this else null
    override val aptPhase: KaptConfiguration.AptPhase? get() = if (hasAptPhase) this else null

    constructor(
        kaptClasspath: List<Path>,
        stubsOutputDir: Path,
        sourcesOutputDir: Path,
        annotationProcessorsClasspath: List<Path>,
    ) : this(
        Options(KaptConfiguration::class),
        kaptClasspath = kaptClasspath.toList(),
        stubsOutputDir = stubsOutputDir,
        sourcesOutputDir = sourcesOutputDir,
        annotationProcessorsClasspath = annotationProcessorsClasspath.toList()
    ) {
        initializeOptions(this::class, options)
    }

    fun <V> get(key: Option<V>): V {
        return options[key]
    }

    fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptConfiguration.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptConfiguration.Option<V>, value: V) {
        options[key] = value
    }

    override fun withAptPhase(): KaptConfiguration.AptPhase.Builder {
        hasAptPhase = true
        return this
    }

    override fun withStubsPhase(): KaptConfiguration.StubsPhase.Builder {
        hasStubsPhase = true
        return this
    }

    override fun toBuilder(): KaptConfiguration.Builder = deepCopy()

    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptConfiguration.StubsPhase.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptConfiguration.AptPhase.Option<V>): V {
        return options[key]
    }

    override fun toCompilerPlugin(): CompilerPlugin {
        val copy = deepCopy()
        copy.set(TOOLS_JAR, null)
        copy.set(STUBS_OUTPUT_DIR, stubsOutputDir)
        copy.set(SOURCE_OUTPUT_DIR, sourcesOutputDir)
        copy.set(ANNOTATION_PROCESSOR_CLASSPATH, annotationProcessorsClasspath)
        return CompilerPlugin(
            PLUGIN_ID,
            classpath = kaptClasspath + (listOfNotNull(get(TOOLS_JAR))),
            rawArguments = copy.toCompilerPluginOptions(),
            orderingRequirements = emptySet(),
        )
    }

    private fun toCompilerPluginOptions(): List<CompilerPluginOption> {
        val compilerPluginOptions = mutableListOf<CompilerPluginOption>()
        val aptMode = when {
            hasAptPhase && hasStubsPhase -> "stubsAndApt"
            hasAptPhase -> "apt"
            hasStubsPhase -> "stubs"
            else -> error("At least one of apt or stubs phase required.")
        }
        compilerPluginOptions.add(CompilerPluginOption("aptMode", aptMode))
        options.iterator().toCompilerPluginOptions(compilerPluginOptions)
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

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptConfiguration.AptPhase.Option<V>, value: V) {
        options[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptConfiguration.StubsPhase.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): KaptConfiguration = deepCopy()

    override fun deepCopy(): KaptConfigurationImpl {
        return KaptConfigurationImpl(
            options.deepCopy(),
            kaptClasspath = kaptClasspath,
            stubsOutputDir = stubsOutputDir,
            sourcesOutputDir = sourcesOutputDir,
            annotationProcessorsClasspath = annotationProcessorsClasspath,
            hasAptPhase,
            hasStubsPhase
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

        // Apt phase options

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

        // Stubs phase options

        val STUBS_OUTPUT_DIR: Option<Path?> = Option("stubs", null)
        val STRIP_METADATA: Option<Boolean> = Option("stripMetadata", false)
        val STRICT_MODE: Option<Boolean> = Option("strict", false)
        val MAP_DIAGNOSTIC_LOCATIONS: Option<Boolean> = Option("mapDiagnosticLocations", false)
    }
}
