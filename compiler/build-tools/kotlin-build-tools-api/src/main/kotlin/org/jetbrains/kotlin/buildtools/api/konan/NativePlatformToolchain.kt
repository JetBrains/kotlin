/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeBinaryCompilationOperation
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativePlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * *.kt -> .klib
     */
    public fun nativeKlibCompilationOperationBuilder(sources: List<Path>, outputKlib: Path): NativeKlibCompilationOperation.Builder

    /**
     * *.klib -> final artifacts (binary + exported headers)
     *
     * @param destinationDirectory the directory where to put the final artifacts
     */
    public fun nativeBinaryCompilationOperationBuilder(
        klibs: List<Path>,
        destinationDirectory: Path,
    ): NativeBinaryCompilationOperation.Builder

    public companion object {
        /**
         * Gets a [NativePlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<NativePlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.native: NativePlatformToolchain get() = getToolchain<NativePlatformToolchain>()
    }
}

@ExperimentalBuildToolsApi
public inline fun NativePlatformToolchain.nativeKlibCompilationOperation(
    sources: List<Path>,
    outputKlib: Path,
    action: NativeKlibCompilationOperation.Builder.() -> Unit,
): NativeKlibCompilationOperation = nativeKlibCompilationOperationBuilder(sources, outputKlib).apply(action).build()

@ExperimentalBuildToolsApi
public inline fun NativePlatformToolchain.nativeBinaryCompilationOperation(
    klibs: List<Path>,
    destinationDirectory: Path,
    action: NativeBinaryCompilationOperation.Builder.() -> Unit,
): NativeBinaryCompilationOperation = nativeBinaryCompilationOperationBuilder(klibs, destinationDirectory).apply(action).build()
