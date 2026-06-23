/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.KaptCompilerPlugin.Option
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface KaptCompilerPlugin {

    public val kaptClasspath: List<Path>
    public val aptPhase: AptPhase?
    public val stubsPhase: StubsPhase?

    public interface Builder {
        public val kaptClasspath: List<Path>
        public var aptPhase: AptPhase?
        public var stubsPhase: StubsPhase?
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun aptPhaseBuilder(): AptPhase.Builder
        public fun stubsPhaseBuilder(): StubsPhase.Builder

        public fun build(): KaptCompilerPlugin
    }

    public fun toBuilder(): Builder

    public fun toCompilerPlugin(): CompilerPlugin

    public operator fun <V> get(key: Option<V>): V

    public class Option<V> internal constructor(
        id: String,
        public val availableSinceVersion: KotlinReleaseVersion,
    ) : BaseOption<V>(id)

    public companion object {
        /**
         * Output path for incremental data.
         */
        @JvmField
        public val INCREMENTAL_DATA_OUTPUT_DIR: Option<Path?> = Option("incrementalData", KotlinReleaseVersion(2, 5, 0))

        /**
         * tools.jar file location (for JDK versions up to 1.8).
         */
        @JvmField
        public val TOOLS_JAR: Option<Path?> = Option("toolsJarLocation", KotlinReleaseVersion(2, 5, 0))

        /**
         * Put initializers on fields when corresponding primary constructor parameters have a default value specified.
         */
        @JvmField
        public val DUMP_DEFAULT_PARAMETER_VALUES: Option<Boolean> = Option("dumpDefaultParameterValues", KotlinReleaseVersion(2, 5, 0))

        /**
         * Enable verbose output.
         */
        @JvmField
        public val VERBOSE: Option<Boolean> = Option("verbose", KotlinReleaseVersion(2, 5, 0))

        /**
         * Show information messages as warnings.
         */
        @JvmField
        public val INFO_AS_WARNINGS: Option<Boolean> = Option("infoAsWarnings", KotlinReleaseVersion(2, 5, 0))
    }

    @ExperimentalBuildToolsApi
    public interface AptPhase {

        public interface Builder {
            public operator fun <V> get(key: Option<V>): V
            public operator fun <V> set(key: Option<V>, value: V)
            public fun build(): AptPhase
        }

        public fun toBuilder(): Builder

        public operator fun <V> get(key: Option<V>): V

        public class Option<V> internal constructor(
            id: String,
            public val availableSinceVersion: KotlinReleaseVersion,
        ) : BaseOption<V>(id)

        public companion object {

            /**
             * Discover annotation processors in compile classpath.
             */
            @JvmField
            public val INCLUDE_COMPILE_CLASSPATH: Option<Boolean> = Option("includeCompileClasspath", KotlinReleaseVersion(2, 5, 0))

            /**
             * Replace generated or error types with ones from the generated sources.
             */
            @JvmField
            public val CORRECT_ERROR_TYPES: Option<Boolean> = Option("correctErrorTypes", KotlinReleaseVersion(2, 5, 0))

            /**
             * Output path for generated sources.
             */
            @JvmField
            public val SOURCE_OUTPUT_DIR: Option<Path?> = Option("sources", KotlinReleaseVersion(2, 5, 0))

            /**
             * Output path for generated classes.
             */
            @JvmField
            public val CLASS_OUTPUT_DIR: Option<Path?> = Option("classes", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Changed java source file that should be processed when using incremental annotation processing.
             */
            @JvmField
            public val CHANGED_FILES: Option<List<Path>?> = Option("changedFile", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Compiled sources (.class files) from previous compilation. This is typically a kotlinc or javac output.
             */
            @JvmField
            public val COMPILED_SOURCES_DIR: Option<List<Path>?> = Option("compiledSourcesDir", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Output directory for cache necessary to support incremental annotation processing.
             */
            @JvmField
            public val INCREMENTAL_CACHE: Option<Path?> = Option("incrementalCache", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Classpath jvm internal names that changed.
             */
            @JvmField
            public val CLASSPATH_CHANGES: Option<List<String>?> = Option("classpathChange", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Enables incremental apt processing.
             */
            @JvmField
            public val PROCESS_INCREMENTALLY: Option<Boolean> = Option("processIncrementally", KotlinReleaseVersion(2, 5, 0))

            /**
             * Annotation processor classpath.
             */
            @JvmField
            public val ANNOTATION_PROCESSOR_CLASSPATH: Option<List<Path>?> = Option("apclasspath", KotlinReleaseVersion(2, 5, 0))

            /**
             * Annotation processor qualified names.
             */
            @JvmField
            public val ANNOTATION_PROCESSORS: Option<List<String>?> = Option("processors", KotlinReleaseVersion(2, 5, 0))

            /**
             * Annotation processor options.
             */
            @JvmField
            public val APT_OPTIONS: Option<Map<String, String>?> = Option("apOption", KotlinReleaseVersion(2, 5, 0))

            /**
             * Javac options.
             */
            @JvmField
            public val JAVAC_OPTIONS: Option<Map<String, String>?> = Option("javacOption", KotlinReleaseVersion(2, 5, 0))

            /**
             * Detect memory leaks in annotation processors.
             */
            @JvmField
            public val DETECT_MEMORY_LEAKS: Option<KaptDetectMemoryLeaksMode> = Option("detectMemoryLeaks", KotlinReleaseVersion(2, 5, 0))

            /**
             * Dump list of files read by processors to the specified file.
             */
            @JvmField
            public val DUMP_FILE_READ_HISTORY: Option<Path?> = Option("dumpFileReadHistory", KotlinReleaseVersion(2, 5, 0))

            /**
             * Show processor timings.
             */
            @JvmField
            public val SHOW_PROCESSOR_STATS: Option<Boolean> = Option("showProcessorStats", KotlinReleaseVersion(2, 5, 0))

            /**
             * Dump processor statistics (performance and generations) to the specified file.
             */
            @JvmField
            public val DUMP_PROCESSOR_STATS: Option<Path?> = Option("dumpProcessorStats", KotlinReleaseVersion(2, 5, 0))
        }
    }


    @ExperimentalBuildToolsApi
    public interface StubsPhase {

        public interface Builder {
            public operator fun <V> get(key: Option<V>): V
            public operator fun <V> set(key: Option<V>, value: V)
            public fun build(): StubsPhase
        }

        public fun toBuilder(): Builder

        public operator fun <V> get(key: Option<V>): V

        public class Option<V> internal constructor(
            id: String,
            public val availableSinceVersion: KotlinReleaseVersion,
        ) : BaseOption<V>(id)

        public companion object {
            /**
             * Output path for Java stubs.
             */
            @JvmField
            public val STUBS_OUTPUT_DIR: Option<Path?> = Option("stubs", KotlinReleaseVersion(2, 5, 0))

            /**
             * Strip @Metadata annotations from stubs.
             */
            @JvmField
            public val STRIP_METADATA: Option<Boolean> = Option("stripMetadata", KotlinReleaseVersion(2, 5, 0))

            /**
             * Show errors on incompatibilities during stub generation.
             */
            @JvmField
            public val STRICT_MODE: Option<Boolean> = Option("strict", KotlinReleaseVersion(2, 5, 0))

            /**
             * Map diagnostic reported on kapt stubs to original locations in Kotlin sources.
             */
            @JvmField
            public val MAP_DIAGNOSTIC_LOCATIONS: Option<Boolean> = Option("mapDiagnosticLocations", KotlinReleaseVersion(2, 5, 0))
        }
    }
}
