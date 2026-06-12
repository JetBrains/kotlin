/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * Thrown when the Kotlin compilation process cannot complete because of an internal failure.
 *
 * This exception is *not* thrown for compilation errors caused by problems in the source code.
 *
 * @param message A description of the exception.
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public class KotlinCompilationProcessFailedException(
    message: String
) : KotlinBuildToolsException(message)
