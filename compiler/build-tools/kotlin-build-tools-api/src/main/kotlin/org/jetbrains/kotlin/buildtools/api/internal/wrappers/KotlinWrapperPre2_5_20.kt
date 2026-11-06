/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class, DelicateBuildToolsApi::class) @file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.cri.CompilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.KaptConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
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
internal class KotlinWrapperPre2_5_20(
    override val base: KotlinToolchains,
) : KotlinToolchains by base, KotlinToolchainsWrapper {

    override val implClassloader: ClassLoader by lazy { getImplClassloader() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T = when (type) {
        JvmPlatformToolchain::class.java -> JvmPlatformToolchainWrapper(base.getToolchain(type))
        JsPlatformToolchain::class.java -> JsPlatformToolchainWrapper(base.getToolchain(type))
        WasmPlatformToolchain::class.java -> WasmPlatformToolchainWrapper(base.getToolchain(type))
        KotlinMetadataPlatformToolchain::class.java -> KotlinMetadataPlatformToolchainWrapper(base.getToolchain(type))
        else -> base.getToolchain(type)
    } as T

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionWrapper(this, base.createBuildSession())
    }

    class BuildSessionWrapper(override val kotlinToolchains: KotlinWrapperPre2_5_20, private val base: KotlinToolchains.BuildSession) :
        KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            return base.executeOperation(realOperation, executionPolicy, logger)
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation.Builder = JvmCompilationOperationBuilderWrapper(
            base.jvmCompilationOperationBuilder(sources, destinationDirectory),
        )

        override fun kaptCompilerPluginBuilder(
            kaptClasspath: List<Path>,
            stubsOutputDir: Path,
            annotationProcessorsClasspath: List<Path>
        ): KaptConfiguration.Builder {
            error("Only available starting from Kotlin compiler 2.5.20")
        }
    }

    private class JvmCompilationOperationBuilderWrapper(private val base: JvmCompilationOperation.Builder) :
        JvmCompilationOperation.Builder by base {
        override fun compilerRefIndexConfigurationBuilder(outputDirectory: Path) = compilerRefIndexConfigurationIsUnsupported()
        override fun build(): JvmCompilationOperation = JvmCompilationOperationWrapper(base.build())
    }

    private class JvmCompilationOperationWrapper(private val base: JvmCompilationOperation) :
        JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): JvmCompilationOperation.Builder = JvmCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class JsPlatformToolchainWrapper(private val base: JsPlatformToolchain) : JsPlatformToolchain by base {
        override fun jsKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): JsKlibCompilationOperation.Builder =
            JsKlibCompilationOperationBuilderWrapper(base.jsKlibCompilationOperationBuilder(sources, destination))

        override fun jsLinkingOperationBuilder(klib: Path, destination: Path): JsLinkingOperation.Builder =
            JsLinkingOperationBuilderWrapper(base.jsLinkingOperationBuilder(klib, destination))

        override fun jsDtsGenerationOperationBuilder(klibs: List<Path>, outputDirectory: Path): JsDtsGenerationOperation.Builder =
            JsDtsGenerationOperationBuilderWrapper(base.jsDtsGenerationOperationBuilder(klibs, outputDirectory))
    }

    private class JsKlibCompilationOperationBuilderWrapper(private val base: JsKlibCompilationOperation.Builder) :
        JsKlibCompilationOperation.Builder by base {
        override fun compilerRefIndexConfigurationBuilder(outputDirectory: Path) = compilerRefIndexConfigurationIsUnsupported()
        override fun build(): JsKlibCompilationOperation = JsKlibCompilationOperationWrapper(base.build())
    }

    private class JsKlibCompilationOperationWrapper(private val base: JsKlibCompilationOperation) :
        JsKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): JsKlibCompilationOperation.Builder = JsKlibCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class JsLinkingOperationBuilderWrapper(private val base: JsLinkingOperation.Builder) : JsLinkingOperation.Builder by base {
        override fun compilerRefIndexConfigurationBuilder(outputDirectory: Path) = compilerRefIndexConfigurationIsUnsupported()
        override fun build(): JsLinkingOperation = JsLinkingOperationWrapper(base.build())
    }

    private class JsLinkingOperationWrapper(val base: JsLinkingOperation) :
        JsLinkingOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): JsLinkingOperation.Builder = JsLinkingOperationBuilderWrapper(base.toBuilder())
    }

    private class JsDtsGenerationOperationBuilderWrapper(private val base: JsDtsGenerationOperation.Builder) :
        JsDtsGenerationOperation.Builder by base {
        // A 2.5.0 implementation class-checks the linking operation, so pass it the wrapped-away implementation operation
        override fun configureFrom(linkingOperation: JsLinkingOperation) {
            base.configureFrom((linkingOperation as? JsLinkingOperationWrapper)?.base ?: linkingOperation)
        }

        override fun build(): JsDtsGenerationOperation = JsDtsGenerationOperationWrapper(base.build())
    }

    private class JsDtsGenerationOperationWrapper(private val base: JsDtsGenerationOperation) :
        JsDtsGenerationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): JsDtsGenerationOperation.Builder = JsDtsGenerationOperationBuilderWrapper(base.toBuilder())
    }

    private class WasmPlatformToolchainWrapper(private val base: WasmPlatformToolchain) : WasmPlatformToolchain by base {
        override fun wasmKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): WasmKlibCompilationOperation.Builder =
            WasmKlibCompilationOperationBuilderWrapper(base.wasmKlibCompilationOperationBuilder(sources, destination))

        override fun wasmLinkingOperationBuilder(klib: Path, destination: Path): WasmLinkingOperation.Builder =
            WasmLinkingOperationBuilderWrapper(base.wasmLinkingOperationBuilder(klib, destination))
    }

    private class WasmKlibCompilationOperationBuilderWrapper(private val base: WasmKlibCompilationOperation.Builder) :
        WasmKlibCompilationOperation.Builder by base {
        override fun compilerRefIndexConfigurationBuilder(outputDirectory: Path) = compilerRefIndexConfigurationIsUnsupported()
        override fun build(): WasmKlibCompilationOperation = WasmKlibCompilationOperationWrapper(base.build())
    }

    private class WasmKlibCompilationOperationWrapper(private val base: WasmKlibCompilationOperation) :
        WasmKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): WasmKlibCompilationOperation.Builder = WasmKlibCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class WasmLinkingOperationBuilderWrapper(private val base: WasmLinkingOperation.Builder) :
        WasmLinkingOperation.Builder by base {
        override fun compilerRefIndexConfigurationBuilder(outputDirectory: Path) = compilerRefIndexConfigurationIsUnsupported()
        override fun build(): WasmLinkingOperation = WasmLinkingOperationWrapper(base.build())
    }

    private class WasmLinkingOperationWrapper(private val base: WasmLinkingOperation) :
        WasmLinkingOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): WasmLinkingOperation.Builder = WasmLinkingOperationBuilderWrapper(base.toBuilder())
    }

    private class KotlinMetadataPlatformToolchainWrapper(private val base: KotlinMetadataPlatformToolchain) :
        KotlinMetadataPlatformToolchain by base {
        override fun metadataKlibCompilationOperationBuilder(
            sources: List<Path>,
            destination: Path,
        ): KotlinMetadataKlibCompilationOperation.Builder =
            KotlinMetadataCompilationOperationBuilderWrapper(base.metadataKlibCompilationOperationBuilder(sources, destination))
    }

    private class KotlinMetadataCompilationOperationBuilderWrapper(private val base: KotlinMetadataKlibCompilationOperation.Builder) :
        KotlinMetadataKlibCompilationOperation.Builder by base {
        override fun compilerRefIndexConfigurationBuilder(outputDirectory: Path) = compilerRefIndexConfigurationIsUnsupported()
        override fun build(): KotlinMetadataKlibCompilationOperation = KotlinMetadataCompilationOperationWrapper(base.build())
    }

    private class KotlinMetadataCompilationOperationWrapper(private val base: KotlinMetadataKlibCompilationOperation) :
        KotlinMetadataKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder(): KotlinMetadataKlibCompilationOperation.Builder =
            KotlinMetadataCompilationOperationBuilderWrapper(base.toBuilder())
    }
}

private fun compilerRefIndexConfigurationIsUnsupported(): CompilerRefIndexConfiguration.Builder =
    error("The compiler reference index configuration is supported from compiler version 2.5.20.")
