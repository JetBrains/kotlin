/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * Thrown when the loaded Build Tools API implementation does not provide the requested toolchain, usually because the Kotlin compiler
 * in use is older than the toolchain. Retrying with the same implementation fails the same way.
 *
 * Implementations before Kotlin 2.5.20 throw a plain [IllegalStateException] in this case, so this exception extends it.
 *
 * @property toolchainType the requested toolchain type
 * @property compilerVersion the version of the loaded Kotlin compiler
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public class UnsupportedToolchainException(
    public val toolchainType: Class<out KotlinToolchains.Toolchain>,
    public val compilerVersion: String,
) : IllegalStateException("Kotlin compiler $compilerVersion does not support the ${toolchainType.name} toolchain.")
