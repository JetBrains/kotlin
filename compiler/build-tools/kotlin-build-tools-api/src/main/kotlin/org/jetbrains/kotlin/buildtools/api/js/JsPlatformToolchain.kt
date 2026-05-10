/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Allows creating operations that can be used for performing Kotlin/JS compilations.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.js].
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public interface JsPlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * Creates a builder for an operation for producing the final output from a klib file
     *
     * @param klib the input klib file or directory that will be used for linking
     * @param destination where to put the output of the compilation
     */
    public fun jsLinkingOperationBuilder(klib: Path, destination: Path): JsLinkingOperation.Builder

    /**
     * Creates a builder for an operation for compiling Kotlin sources into a klib.
     *
     * @param sources all sources of the compilation unit
     * @param destination output klib file or directory
     *
     */
    public fun jsKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): JsKlibCompilationOperation.Builder

    public companion object {
        /**
         * Gets a [JsPlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<JsPlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.js: JsPlatformToolchain get() = getToolchain<JsPlatformToolchain>()
    }
}

/**
 * Convenience function for creating a [JsLinkingOperation] with options configured by [builderAction].
 *
 * @return an immutable `JsCompilationOperation`.
 * @see JsPlatformToolchain.jsLinkingOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JsPlatformToolchain.jsLinkingOperation(
    sources: Path,
    destinationDirectory: Path,
    builderAction: JsLinkingOperation.Builder.() -> Unit = {},
): JsLinkingOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return jsLinkingOperationBuilder(sources, destinationDirectory).apply(builderAction).build()
}

/**
 * Convenience function for creating a [JsKlibCompilationOperation] with options configured by [builderAction].
 *
 * @return an immutable `JsKlibCompilationOperation`.
 * @see JsPlatformToolchain.jsKlibCompilationOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JsPlatformToolchain.jsKlibCompilationOperation(
    sources: List<Path>,
    destinationKlib: Path,
    builderAction: JsKlibCompilationOperation.Builder.() -> Unit = {},
): JsKlibCompilationOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return jsKlibCompilationOperationBuilder(sources, destinationKlib).apply(builderAction).build()
}
