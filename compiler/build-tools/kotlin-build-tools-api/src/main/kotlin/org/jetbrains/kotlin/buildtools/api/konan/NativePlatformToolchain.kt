/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Allows creating operations that can be used for performing Kotlin/Native compilations.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.native].
 *
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface NativePlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * Creates a builder for an operation for compiling Kotlin sources into a Kotlin/Native klib.
     *
     * @param sources all sources of the compilation unit
     * @param destination output klib file or directory
     */
    public fun nativeKlibCompilationOperationBuilder(
        sources: List<Path>,
        destination: Path,
    ): NativeKlibCompilationOperation.Builder

    public companion object {
        /**
         * Gets a [NativePlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<KotlinNativePlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.native: NativePlatformToolchain get() = getToolchain<NativePlatformToolchain>()
    }
}

/**
 * Convenience function for creating a [NativeKlibCompilationOperation] with options configured by [builderAction].
 *
 * @return an immutable `KotlinNativeKlibCompilationOperation`.
 * @see NativePlatformToolchain.nativeKlibCompilationOperationBuilder
 * @since 2.5.20
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun NativePlatformToolchain.nativeKlibCompilationOperation(
    sources: List<Path>,
    destinationKlib: Path,
    builderAction: NativeKlibCompilationOperation.Builder.() -> Unit = {},
): NativeKlibCompilationOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return nativeKlibCompilationOperationBuilder(sources, destinationKlib).apply(builderAction).build()
}
