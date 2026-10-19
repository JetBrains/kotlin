/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Serializable
internal abstract class CancellableBuildOperationImpl<R> : BuildOperationImpl<R>(), CancellableBuildOperation<R> {
    @Transient
    private val isCancelled: AtomicBoolean = AtomicBoolean(false)

    @Transient
    private val onCancelAction: AtomicReference<(() -> Unit)?> = AtomicReference(null)

    override fun cancel() {
        val wasCancelled = isCancelled.exchange(true)
        if (!wasCancelled) {
            onCancelAction.load()?.invoke()
        }
    }

    fun onCancel(action: () -> Unit) {
        requireNotCancelled()
        val actionWasSet = onCancelAction.compareAndSet(null, action)
        check(actionWasSet) { "onCancel action was already set. Setting it again is an error." }
    }

    @Transient
    protected val cancellationHandle = object : CompilationCanceledStatus {
        override fun checkCanceled() {
            if (isCancelled.load()) {
                throw CompilationCanceledException()
            }
        }
    }

    abstract fun executeCancellableImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): R

    final override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): R {
        val returnValue = executeCancellableImpl(projectId, executionPolicy, logger, executionContext)
        requireNotCancelled()
        return returnValue
    }

    private fun requireNotCancelled() {
        if (isCancelled.load()) {
            throw OperationCancelledException()
        }
    }
}
