/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

/**
 * See [Module.executeCompiledClass]
 */
class ExecutionOutcome(
    /**
     * Contains mixed stdout and stderr (usually the sequence of events is more important than the organization of output streams)
     */
    val output: List<String>,
    /**
     * [isComplete] is true, if execution appears to be successful (aka exitCode=0)
     */
    val isComplete: Boolean,
    /**
     * Any exception caught while creating and waiting for the execution process
     *
     * It's a bit hard to split ExecutionOutcome into [Either] class, because outputs might be valuable on failure
     */
    val failureReason: Throwable?,
)
