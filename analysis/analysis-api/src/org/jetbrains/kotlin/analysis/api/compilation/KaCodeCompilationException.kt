/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

/**
 * Thrown when an exception occurred while analyzing the code to be compiled, or during target platform code generation.
 *
 * @see compile
 */
@KaExperimentalApi
public class KaCodeCompilationException(cause: Throwable) : RuntimeException(cause)
