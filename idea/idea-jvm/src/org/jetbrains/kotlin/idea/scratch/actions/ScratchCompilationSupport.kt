/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import org.jetbrains.kotlin.idea.scratch.ScratchExecutor
import org.jetbrains.kotlin.idea.scratch.ScratchFile

object ScratchCompilationSupport {
    private var inProgress = false

    private var file: ScratchFile? = null
    private var executor: ScratchExecutor? = null

    fun isInProgress(file: ScratchFile): Boolean = inProgress && this.file == file
    fun isAnyInProgress(): Boolean = inProgress

    fun start(file: ScratchFile, executor: ScratchExecutor) {
        this.inProgress = true

        this.file = file
        this.executor = executor
    }

    fun stop() {
        this.inProgress = false

        this.file = null
        this.executor = null
    }

    fun forceStop() {
        this.executor?.stop()

        stop()
    }
}