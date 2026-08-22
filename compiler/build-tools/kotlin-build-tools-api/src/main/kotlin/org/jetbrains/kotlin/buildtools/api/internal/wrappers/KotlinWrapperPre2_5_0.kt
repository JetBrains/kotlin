/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class, ExperimentalCompilerArgument::class)
@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.ENABLE_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule as JsIncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule as WasmIncrementalModule
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import java.nio.file.Path

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre Kotlin 2.5.0.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
@Suppress("ClassName")
internal class KotlinWrapperPre2_5_0(
    private val base: KotlinToolchains,
) : KotlinToolchains by base {

    @Suppress("UNCHECKED_CAST")
    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T = when (type) {
        JvmPlatformToolchain::class.java -> JvmPlatformToolchainWrapper(base.getToolchain(type))
        JsPlatformToolchain::class.java -> JsPlatformToolchainWrapper(base.getToolchain(type))
        WasmPlatformToolchain::class.java -> WasmPlatformToolchainWrapper(base.getToolchain(type))
        else -> base.getToolchain(type)
    } as T

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionWrapper(this, base.createBuildSession())
    }

    class BuildSessionWrapper(override val kotlinToolchains: KotlinWrapperPre2_5_0, private val base: KotlinToolchains.BuildSession) :
        KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            // Unwrap so the pre-2.4.0 executeOperation can handle its own type check
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            return base.executeOperation(realOperation, executionPolicy, logger)
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation.Builder =
            JvmCompilationOperationBuilderWrapper(base.jvmCompilationOperationBuilder(sources, destinationDirectory))
    }

    private class JvmCompilationOperationBuilderWrapper(
        private val base: JvmCompilationOperation.Builder,
    ) : JvmCompilationOperation.Builder by base {
        override fun snapshotBasedIcConfigurationBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
        ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder =
            JvmIcConfigurationBuilderWrapper(
                base.snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles),
            )

        @Deprecated(
            "The shrunkClasspathSnapshot parameter is no longer required",
            replaceWith = ReplaceWith("snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles)"),
            level = DeprecationLevel.ERROR,
        )
        override fun snapshotBasedIcConfigurationBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
            shrunkClasspathSnapshot: Path,
        ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder =
            JvmIcConfigurationBuilderWrapper(
                base.snapshotBasedIcConfigurationBuilder(
                    workingDirectory,
                    sourcesChanges,
                    dependenciesSnapshotFiles,
                    shrunkClasspathSnapshot,
                ),
            )

        override fun build(): JvmCompilationOperation = JvmCompilationOperationWrapper(base.build())
    }

    private class JvmCompilationOperationWrapper(
        private val base: JvmCompilationOperation,
    ) : JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): JvmCompilationOperation.Builder = JvmCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class JsPlatformToolchainWrapper(private val base: JsPlatformToolchain) : JsPlatformToolchain by base {
        override fun jsKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): JsKlibCompilationOperation.Builder =
            JsKlibCompilationOperationBuilderWrapper(base.jsKlibCompilationOperationBuilder(sources, destination))
    }

    private class JsKlibCompilationOperationBuilderWrapper(
        private val base: JsKlibCompilationOperation.Builder,
    ) : JsKlibCompilationOperation.Builder by base {
        override fun historyBasedIcConfigurationBuilder(
            rootProjectDir: Path,
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            modulesInformation: List<JsIncrementalModule>,
        ): JsHistoryBasedIncrementalCompilationConfiguration.Builder =
            JsIcConfigurationBuilderWrapper(
                base.historyBasedIcConfigurationBuilder(rootProjectDir, workingDirectory, sourcesChanges, modulesInformation),
            )

        override fun build(): JsKlibCompilationOperation = JsKlibCompilationOperationWrapper(base.build())
    }

    private class JsKlibCompilationOperationWrapper(
        private val base: JsKlibCompilationOperation,
    ) : JsKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): JsKlibCompilationOperation.Builder = JsKlibCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class WasmPlatformToolchainWrapper(private val base: WasmPlatformToolchain) : WasmPlatformToolchain by base {
        override fun wasmKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): WasmKlibCompilationOperation.Builder =
            WasmKlibCompilationOperationBuilderWrapper(base.wasmKlibCompilationOperationBuilder(sources, destination))
    }

    private class WasmKlibCompilationOperationBuilderWrapper(
        private val base: WasmKlibCompilationOperation.Builder,
    ) : WasmKlibCompilationOperation.Builder by base {
        override fun historyBasedIcConfigurationBuilder(
            rootProjectDir: Path,
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            modulesInformation: List<WasmIncrementalModule>,
        ): WasmHistoryBasedIncrementalCompilationConfiguration.Builder =
            WasmIcConfigurationBuilderWrapper(
                base.historyBasedIcConfigurationBuilder(rootProjectDir, workingDirectory, sourcesChanges, modulesInformation),
            )

        override fun build(): WasmKlibCompilationOperation = WasmKlibCompilationOperationWrapper(base.build())
    }

    private class WasmKlibCompilationOperationWrapper(
        private val base: WasmKlibCompilationOperation,
    ) : WasmKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): WasmKlibCompilationOperation.Builder = WasmKlibCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class JvmIcConfigurationBuilderWrapper(
        private val base: JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
    ) : JvmSnapshotBasedIncrementalCompilationConfiguration.Builder by base {
        override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = base[key.toPre2_5_0Option()]

        override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
            base[key.toPre2_5_0Option()] = value
        }
    }

    private class JsIcConfigurationBuilderWrapper(
        private val base: JsHistoryBasedIncrementalCompilationConfiguration.Builder,
    ) : JsHistoryBasedIncrementalCompilationConfiguration.Builder by base {
        override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = base[key.toPre2_5_0Option()]

        override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
            base[key.toPre2_5_0Option()] = value
        }
    }

    private class WasmIcConfigurationBuilderWrapper(
        private val base: WasmHistoryBasedIncrementalCompilationConfiguration.Builder,
    ) : WasmHistoryBasedIncrementalCompilationConfiguration.Builder by base {
        override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = base[key.toPre2_5_0Option()]

        override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
            base[key.toPre2_5_0Option()] = value
        }
    }
}

/**
 * The option that carried [ENABLE_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES]'s setting before 2.5.0.
 *
 * It is declared here rather than referenced from [BaseIncrementalCompilationConfiguration] on purpose: the option is
 * being removed from the API surface, while implementations older than 2.5.0 keep reading the setting under this id.
 * Keeping a private copy lets this wrapper outlive the public declaration.
 *
 * It is a getter rather than a stored property because a static `Option` field would make this file look like a public
 * option holder to the reflection-based discovery in `AvailableSinceTest`. Options are keyed by their
 * [id][org.jetbrains.kotlin.buildtools.api.internal.BaseOption.id], so a fresh instance per access behaves identically.
 */
private val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: BaseIncrementalCompilationConfiguration.Option<Boolean>
    get() = BaseIncrementalCompilationConfiguration.Option(
        "UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM",
        KotlinReleaseVersion(2, 3, 0),
    )

/**
 * [ENABLE_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES] is available only since 2.5.0, so passing it to an older
 * implementation fails its option availability check. Such implementations read the same setting from
 * [UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] instead.
 *
 * Both options hold a `Boolean`, so substituting one for the other keeps the value type intact.
 */
@Suppress("UNCHECKED_CAST")
private fun <V> BaseIncrementalCompilationConfiguration.Option<V>.toPre2_5_0Option(): BaseIncrementalCompilationConfiguration.Option<V> =
    if (id == ENABLE_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES.id) {
        UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM as BaseIncrementalCompilationConfiguration.Option<V>
    } else {
        this
    }
