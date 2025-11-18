/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.lang.RuntimeException

/**
 * Thrown to indicate that a build operation was cancelled before completion.
 *
 * This exception is used to signal that an operation was intentionally cancelled.
 * It may be thrown when a [CancellableBuildOperation.cancel] request is made during the execution of a [CancellableBuildOperation].
 *
 * Note: The behavior of cancellation and its guarantees are dependent on the specific operation
 * implementation used.
 *
 * @param message A detailed message describing the cancellation (default: "Operation has been cancelled.")
 * @param cause The cause of the cancellation or an exception triggering it (default: `null`)
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public class OperationCancelledException(message: String = "Operation has been cancelled.", cause: Throwable? = null) :
    RuntimeException(message, cause)