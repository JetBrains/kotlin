/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeCompilationOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeDownloadDependenciesOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeLinkingOperation
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativePlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * *.kt -> .klib
     */
    public fun nativeCompilationOperationBuilder(sources: List<Path>, ouptutKlib: Path): NativeCompilationOperation.Builder

    /**
     * *.klib -> final artifacts (binary + exported headers)
     *
     * @param destinationDirectory the directory where to put the final artifacts
     */
    public fun nativeLinkingOperationBuilder(
        klibs: List<Path>,
        destinationDirectory: Path,
    ): NativeLinkingOperation.Builder

    public fun nativeDownloadDependenciesOperationBuilder(
        dependenciesDirectory: Path
    ): NativeDownloadDependenciesOperation.Builder

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
public inline fun NativePlatformToolchain.nativeCompilationOperation(
    sources: List<Path>,
    outputKlib: Path,
    action: NativeCompilationOperation.Builder.() -> Unit,
): NativeCompilationOperation = nativeCompilationOperationBuilder(sources, outputKlib).apply(action).build()

@ExperimentalBuildToolsApi
public inline fun NativePlatformToolchain.nativeLinkingOperation(
    klibs: List<Path>,
    destinationDirectory: Path,
    action: NativeLinkingOperation.Builder.() -> Unit,
): NativeLinkingOperation = nativeLinkingOperationBuilder(klibs, destinationDirectory).apply(action).build()

@ExperimentalBuildToolsApi
public inline fun NativePlatformToolchain.nativeDownloadDependenciesOperation(
    klibs: List<Path>,
    destinationDirectory: Path,
    action: NativeDownloadDependenciesOperation.Builder.() -> Unit,
): NativeDownloadDependenciesOperation = nativeDownloadDependenciesOperationBuilder(destinationDirectory).apply(action).build()
