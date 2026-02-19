/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode

data class CustomKlibCompilerException(
    val exitCode: ExitCode,
    val compilerOutput: String,
) : Exception("Compilation failed with exit code $exitCode\n$compilerOutput")
