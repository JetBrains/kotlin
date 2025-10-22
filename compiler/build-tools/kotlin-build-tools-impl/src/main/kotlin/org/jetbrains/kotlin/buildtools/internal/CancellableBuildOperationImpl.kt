/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CancellableBuildOperation
import org.jetbrains.kotlin.daemon.common.makeAutodeletingFlagFile
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal abstract class CancellableBuildOperationImpl<R>() :
    BuildOperationImpl<R>(), CancellableBuildOperation<R> {
    private val isCancelled: AtomicBoolean = AtomicBoolean(false)
    private val compilationAliveFile: File = makeAutodeletingFlagFile(keyword = "compilation")

    protected val compilationAliveFilePath: String get() = compilationAliveFile.absolutePath

    override fun cancel() {
        val wasCancelled = isCancelled.exchange(true)
        if (!wasCancelled) {
            compilationAliveFile.delete()
        }
    }

    protected val cancellationHandle = object : CompilationCanceledStatus {
        override fun checkCanceled() {
            if (isCancelled.load()) {
                throw CompilationCanceledException()
            }
        }
    }
}