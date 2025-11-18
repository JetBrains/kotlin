/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CancellableBuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
internal abstract class CancellableBuildOperationImpl<R> : BuildOperationImpl<R>(), CancellableBuildOperation<R> {
    private val isCancelled: AtomicBoolean = AtomicBoolean(false)
    private val onCancelAction: AtomicReference<(() -> Unit)?> = AtomicReference(null)
    protected val compilationId: Int = compilationIdCounter.incrementAndFetch()

    override fun cancel() {
        val wasCancelled = isCancelled.exchange(true)
        if (!wasCancelled) {
            onCancelAction.load()?.invoke()
        }
    }

    fun onCancel(action: () -> Unit) {
        val actionWasSet = onCancelAction.compareAndSet(null, action)
        check(actionWasSet) { "onCancel action was already set. Setting it again is an error." }
    }

    protected val cancellationHandle = object : CompilationCanceledStatus {
        override fun checkCanceled() {
            if (isCancelled.load()) {
                throw CompilationCanceledException()
            }
        }
    }

    companion object {
        private val compilationIdCounter = AtomicInt(0)
    }

    abstract fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): R

    final override fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
        val returnValue = executeImpl(projectId, executionPolicy, logger)
        return if (isCancelled.load()) {
            throw OperationCancelledException()
        } else {
            returnValue
        }
    }
}