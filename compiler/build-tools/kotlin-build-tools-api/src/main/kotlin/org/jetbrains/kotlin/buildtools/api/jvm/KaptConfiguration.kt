/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

/**
 * Represents the configuration for the Kotlin Annotation Processing Tool (KAPT).
 *
 * This interface provides methods and properties for defining and manipulating
 * the configuration required during annotation processing.
 *
 * Usage example:
 * ```kotlin
 * val kaptConfiguration = jvmCompilationOperationBuilder
 *     .kaptCompilerPluginBuilder()
 *     .withAptPhase()
 *     .apply {
 *         this[KaptConfiguration.AptPhase.ANNOTATION_PROCESSORS] = listOf("com.example.MyProcessor")
 *         // configure other options
 *     }
 *     .build()
 *
 * val kaptCompilerPlugin = kaptConfiguration.toCompilerPlugin()
 *
 * jvmCompilationOperationBuilder.compilerArguments[
 *     CommonCompilerArguments.COMPILER_PLUGINS
 * ] = listOf(kaptCompilerPlugin)
 * ```
 *
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public interface KaptConfiguration {
    public val kaptClasspath: List<Path>

    /**
     * Output directory for Java stubs.
     *
     * @since 2.5.0
     */
    public val stubsOutputDir: Path

    /**
     * Annotation processors classpath.
     *
     * @since 2.5.0
     */
    public val annotationProcessorsClasspath: List<Path>

    /**
     * Output directory for generated sources.
     *
     * @since 2.5.0
     */
    public val sourcesOutputDir: Path

    public val aptPhase: AptPhase?
    public val stubsPhase: StubsPhase?

    /**
     * Represents a builder interface for configuring a KAPT-based configuration.
     *
     * Provides methods to manage and modify configuration options and phase-specific builders.
     *
     * @since 2.5.0
     */
    public interface Builder {
        /**
         * Represents the classpath required for the KAPT (Kotlin Annotation Processing Tool) configuration.
         *
         * This classpath typically contains the KAPT artifact and its dependencies. Annotation processors typically don't need to be part
         * of this classpath, as they are specified in the [AptPhase.Builder] options.
         *
         * @since 2.5.0
         */
        public val kaptClasspath: List<Path>

        /**
         * Retrieves the value associated with the given key of type Option<V>.
         *
         * @param key The option key used to fetch the associated value.
         * @return The value associated with the provided key.
         * @since 2.5.0
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Associates the specified value with the given key in this collection.
         *
         * @param key The key of type Option<V> for which the value is to be set.
         * @param value The value of type V to associate with the specified key.
         * @since 2.5.0
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Enables the 'apt' phase in this KAPT configuration and returns a view on this builder that lets you configure options for it.
         *
         * @return an [AptPhase.Builder] that can be used to further configure this KAPT configuration.
         * @since 2.5.0
         */
        public fun withAptPhase(): AptPhase.Builder

        /**
         * Enables the 'stubs' phase in this KAPT configuration and returns a view on this builder that lets you configure options for it.
         *
         * @return an [StubsPhase.Builder] that can be used to further configure this KAPT configuration.
         * @since 2.5.0
         */
        public fun withStubsPhase(): StubsPhase.Builder
    }

    public fun toBuilder(): Builder

    /**
     * Converts the current configuration into an instance of [CompilerPlugin].
     *
     * @return a configured [CompilerPlugin] instance based on the current state of the `KaptConfiguration` object.
     * @since 2.5.0
     */
    public fun toCompilerPlugin(): CompilerPlugin

    /**
     * Retrieves the value associated with the given key of type Option<V>.
     *
     * @param key The option key used to fetch the associated value.
     * @return The value associated with the provided key.
     * @since 2.5.0
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * An option that can be used to configure various aspects of the Kotlin annotation processing tool (KAPT).
     *
     * @see [Companion]
     * @since 2.5.0
     */
    public class Option<V> internal constructor(
        id: String,
        public val availableSinceVersion: KotlinReleaseVersion,
    ) : BaseOption<V>(id)

    public companion object {
        /**
         * Output path for incremental data.
         *
         * @since 2.5.0
         */
        @JvmField
        public val INCREMENTAL_DATA_OUTPUT_DIR: Option<Path?> = Option("incrementalData", KotlinReleaseVersion(2, 5, 0))

        /**
         * tools.jar file location (for JDK versions up to 1.8).
         *
         * @since 2.5.0
         */
        @JvmField
        public val TOOLS_JAR: Option<Path?> = Option("toolsJarLocation", KotlinReleaseVersion(2, 5, 0))

        /**
         * Put initializers on fields when corresponding primary constructor parameters have a default value specified.
         *
         * @since 2.5.0
         */
        @JvmField
        public val DUMP_DEFAULT_PARAMETER_VALUES: Option<Boolean> = Option("dumpDefaultParameterValues", KotlinReleaseVersion(2, 5, 0))

        /**
         * Enable verbose output.
         *
         * @since 2.5.0
         */
        @JvmField
        public val VERBOSE: Option<Boolean> = Option("verbose", KotlinReleaseVersion(2, 5, 0))

        /**
         * Show information messages as warnings.
         *
         * @since 2.5.0
         */
        @JvmField
        public val INFO_AS_WARNINGS: Option<Boolean> = Option("infoAsWarnings", KotlinReleaseVersion(2, 5, 0))
    }

    /**
     * Represents the annotation processing (APT) phase of the KAPT (Kotlin Annotation Processing Tool).
     *
     * This interface provides access to various options and configurations specific to the APT phase,
     * allowing users to query how annotation processors are executed during the compilation process.
     *
     * @since 2.5.0
     */
    @ExperimentalBuildToolsApi
    public interface AptPhase {

        /**
         * Represents the annotation processing (APT) phase of the KAPT (Kotlin Annotation Processing Tool).
         *
         * This interface provides access to various options and configurations specific to the APT phase,
         * allowing users to query and modify how annotation processors are executed during the compilation process.
         *
         * @since 2.5.0
         */
        public interface Builder : KaptConfiguration.Builder {
            /**
             * Retrieves the value associated with the given key of type Option<V>.
             *
             * @param key The option key used to fetch the associated value.
             * @return The value associated with the provided key.
             * @since 2.5.0
             */
            public operator fun <V> get(key: Option<V>): V

            /**
             * Associates the specified value with the given key in this collection.
             *
             * @param key The key of type Option<V> for which the value is to be set.
             * @param value The value of type V to associate with the specified key.
             */
            public operator fun <V> set(key: Option<V>, value: V)

            /**
             * Builds and returns a configured instance of KaptConfiguration.
             *
             * @return A KaptConfiguration object representing the configuration parameters.
             * @since 2.5.0
             */
            public fun build(): KaptConfiguration
        }

        /**
         * Retrieves the value associated with the given key of type Option<V>.
         *
         * @param key The option key used to fetch the associated value.
         * @return The value associated with the provided key.
         * @since 2.5.0
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * An option that can be used to configure various aspects of the Kotlin annotation processing tool (KAPT).
         *
         * @see [Companion]
         * @since 2.5.0
         */
        public class Option<V> internal constructor(
            id: String,
            public val availableSinceVersion: KotlinReleaseVersion,
        ) : BaseOption<V>(id)

        public companion object {

            /**
             * Discover annotation processors in compile classpath.
             *
             * @since 2.5.0
             */
            @JvmField
            public val INCLUDE_COMPILE_CLASSPATH: Option<Boolean> = Option("includeCompileClasspath", KotlinReleaseVersion(2, 5, 0))

            /**
             * Replace generated or error types with ones from the generated sources.
             *
             * @since 2.5.0
             */
            @JvmField
            public val CORRECT_ERROR_TYPES: Option<Boolean> = Option("correctErrorTypes", KotlinReleaseVersion(2, 5, 0))

            /**
             * Output path for generated classes.
             *
             * If not set, defaults to the [org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.destinationDirectory].
             *
             * @since 2.5.0
             */
            @JvmField
            public val CLASS_OUTPUT_DIR: Option<Path?> = Option("classes", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Changed java source file that should be processed when using incremental annotation processing.
             *
             * @since 2.5.0
             */
            @JvmField
            public val CHANGED_FILES: Option<List<Path>?> = Option("changedFile", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Compiled sources (.class files) from previous compilation. This is typically a kotlinc or javac output.
             *
             * @since 2.5.0
             */
            @JvmField
            public val COMPILED_SOURCES_DIR: Option<List<Path>?> = Option("compiledSourcesDir", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Output directory for cache necessary to support incremental annotation processing.
             *
             * @since 2.5.0
             */
            @JvmField
            public val INCREMENTAL_CACHE: Option<Path?> = Option("incrementalCache", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Classpath jvm internal names that changed.
             *
             * @since 2.5.0
             */
            @JvmField
            public val CLASSPATH_CHANGES: Option<List<String>?> = Option("classpathChange", KotlinReleaseVersion(2, 5, 0))

            /**
             * Use only in apt mode. Enables incremental apt processing.
             *
             * @since 2.5.0
             */
            @JvmField
            public val PROCESS_INCREMENTALLY: Option<Boolean> = Option("processIncrementally", KotlinReleaseVersion(2, 5, 0))


            /**
             * Annotation processor qualified names.
             *
             * @since 2.5.0
             */
            @JvmField
            public val ANNOTATION_PROCESSORS: Option<List<String>?> = Option("processors", KotlinReleaseVersion(2, 5, 0))

            /**
             * Annotation processor options.
             *
             * @since 2.5.0
             */
            @JvmField
            public val APT_OPTIONS: Option<Map<String, String>?> = Option("apOption", KotlinReleaseVersion(2, 5, 0))

            /**
             * Javac options.
             *
             * @since 2.5.0
             */
            @JvmField
            public val JAVAC_OPTIONS: Option<Map<String, String>?> = Option("javacOption", KotlinReleaseVersion(2, 5, 0))

            /**
             * Detect memory leaks in annotation processors.
             *
             * @since 2.5.0
             */
            @JvmField
            public val DETECT_MEMORY_LEAKS: Option<KaptDetectMemoryLeaksMode> = Option("detectMemoryLeaks", KotlinReleaseVersion(2, 5, 0))

            /**
             * Dump list of files read by processors to the specified file.
             *
             * @since 2.5.0
             */
            @JvmField
            public val DUMP_FILE_READ_HISTORY: Option<Path?> = Option("dumpFileReadHistory", KotlinReleaseVersion(2, 5, 0))

            /**
             * Show processor timings.
             *
             * @since 2.5.0
             */
            @JvmField
            public val SHOW_PROCESSOR_STATS: Option<Boolean> = Option("showProcessorStats", KotlinReleaseVersion(2, 5, 0))

            /**
             * Dump processor statistics (performance and generations) to the specified file.
             *
             * @since 2.5.0
             */
            @JvmField
            public val DUMP_PROCESSOR_STATS: Option<Path?> = Option("dumpProcessorStats", KotlinReleaseVersion(2, 5, 0))

            /**
             * Map diagnostic reported on kapt stubs to original locations in Kotlin sources.
             *
             * @since 2.5.0
             */
            @JvmField
            public val MAP_DIAGNOSTIC_LOCATIONS: Option<Boolean> = Option("mapDiagnosticLocations", KotlinReleaseVersion(2, 5, 0))
        }
    }

    /**
     * Represents the `stubs` phase of the KAPT (Kotlin Annotation Processing Tool).
     *
     * This interface provides access to various options and configurations specific to the `stubs` phase,
     * allowing users to query how stubs are generated during the compilation process.
     *
     * @since 2.5.0
     */
    @ExperimentalBuildToolsApi
    public interface StubsPhase {

        /**
         * Represents the `stubs` phase of the KAPT (Kotlin Annotation Processing Tool).
         *
         * This interface provides access to various options and configurations specific to the `stubs` phase,
         * allowing users to query and modify how stubs are generated during the compilation process.
         *
         * @since 2.5.0
         */
        public interface Builder : KaptConfiguration.Builder {
            /**
             * Retrieves the value associated with the given key of type Option<V>.
             *
             * @param key The option key used to fetch the associated value.
             * @return The value associated with the provided key.
             */
            public operator fun <V> get(key: Option<V>): V

            /**
             * Associates the specified value with the given key in this collection.
             *
             * @param key The key of type Option<V> for which the value is to be set.
             * @param value The value of type V to associate with the specified key.
             * @since 2.5.0
             */
            public operator fun <V> set(key: Option<V>, value: V)

            /**
             * Builds and returns a configured instance of KaptConfiguration.
             *
             * @return A KaptConfiguration object representing the configuration parameters.
             */
            public fun build(): KaptConfiguration
        }

        /**
         * Retrieves the value associated with the given key of type Option<V>.
         *
         * @param key The option key used to fetch the associated value.
         * @return The value associated with the provided key.
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * An option that can be used to configure various aspects of the Kotlin annotation processing tool (KAPT).
         *
         * @see [Companion]
         */
        public class Option<V> internal constructor(
            id: String,
            public val availableSinceVersion: KotlinReleaseVersion,
        ) : BaseOption<V>(id)

        public companion object {
            /**
             * Strip @Metadata annotations from stubs.
             *
             * @since 2.5.0
             */
            @JvmField
            public val STRIP_METADATA: Option<Boolean> = Option("stripMetadata", KotlinReleaseVersion(2, 5, 0))

            /**
             * Show errors on incompatibilities during stub generation.
             *
             * @since 2.5.0
             */
            @JvmField
            public val STRICT_MODE: Option<Boolean> = Option("strict", KotlinReleaseVersion(2, 5, 0))
        }
    }
}
