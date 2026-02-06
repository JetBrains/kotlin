/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeCInteropOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeCompilationOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeLinkingOperation
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativePlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * *.kt -> .klib
     *
     * @param destinationDirectory the directory where to put .klib
     */
    public fun createNativeCompilationOperation(sources: List<Path>, destinationDirectory: Path): NativeCompilationOperation

    /**
     * .def -> .klib
     *
     * @param destinationDirectory the directory where to put .klib
     */
    public fun createNativeCInteropOperation(defFile: Path, destinationDirectory: Path): NativeCInteropOperation

    /**
     * *.klib -> final artifacts (binary + exported headers)
     *
     * @param destinationDirectory the directory where to put the final artifacts
     */
    public fun createNativeLinkingOperation(
        klibs: List<Path>,
        destinationDirectory: Path,
    ): NativeLinkingOperation

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