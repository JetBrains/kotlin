/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.intellij.util.containers.HashSetQueue
import org.jetbrains.kotlin.idea.core.script.debug
import java.util.*

/**
 * Sequentially loads script configuration in background.
 * Loading tasks scheduled by calling [ensureScheduled].
 *
 * Progress indicator will be shown after [PROGRESS_INDICATOR_DELAY] ms or if
 * more then [PROGRESS_INDICATOR_MIN_QUEUE] tasks scheduled.
 *
 * States:
 *                                 silentWorker     underProgressWorker
 * - sleep
 * - silent                             x
 * - silent and under progress          x                 x
 * - under progress                                       x
 */
internal class DefaultBackgroundExecutor(
    val project: Project,
    val rootsManager: ScriptClassRootsIndexer
) : BackgroundExecutor {
    companion object {
        const val PROGRESS_INDICATOR_DELAY = 1000
        const val PROGRESS_INDICATOR_MIN_QUEUE = 3
    }

    private val work = Any()
    private val queue: Queue<LoadTask> = HashSetQueue()

    /**
     * Let's fix queue size when progress bar displayed.
     * Progress for rest items will be counted from zero
     */
    private var currentProgressSize: Int = 0
    private var currentProgressDone: Int = 0

    private var silentWorker: SilentWorker? = null
    private var underProgressWorker: UnderProgressWorker? = null
    private val longRunningAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    private var longRunningAlaramRequested = false

    private var inTransaction: Boolean = false
    private var currentFile: VirtualFile? = null

    class LoadTask(val key: VirtualFile, val actions: () -> Unit) {
        override fun equals(other: Any?) =
            this === other || (other is LoadTask && key == other.key)

        override fun hashCode() = key.hashCode()
    }

    @Synchronized
    override fun ensureScheduled(key: VirtualFile, actions: () -> Unit) {
        val task = LoadTask(key, actions)

        if (queue.add(task)) {
            debug(task.key) { "added to update queue" }

            // If the queue is longer than PROGRESS_INDICATOR_MIN_QUEUE, show progress and cancel button
            if (queue.size > PROGRESS_INDICATOR_MIN_QUEUE) {
                requireUnderProgressWorker()
            } else {
                requireSilentWorker()

                if (!longRunningAlaramRequested) {
                    longRunningAlaramRequested = true
                    longRunningAlarm.addRequest(
                        {
                            longRunningAlaramRequested = false
                            requireUnderProgressWorker()
                        },
                        PROGRESS_INDICATOR_DELAY
                    )
                }
            }
        }
    }

    @Synchronized
    private fun requireSilentWorker() {
        if (silentWorker == null && underProgressWorker == null) {
            silentWorker = SilentWorker().also { it.start() }
        }
    }

    @Synchronized
    private fun requireUnderProgressWorker() {
        if (queue.isEmpty() && silentWorker == null) return

        silentWorker?.stopGracefully()
        if (underProgressWorker == null) {
            underProgressWorker = UnderProgressWorker().also { it.start() }
            restartProgressBar()
            updateProgress()
        }
    }

    @Synchronized
    private fun restartProgressBar() {
        currentProgressSize = queue.size
        currentProgressDone = 0
    }

    @Synchronized
    fun updateProgress() {
        underProgressWorker?.progressIndicator?.let {
            it.text2 = currentFile?.path ?: ""
            if (queue.size == 0) {
                // last file
                it.isIndeterminate = true
            } else {
                it.isIndeterminate = false
                if (currentProgressDone > currentProgressSize) {
                    restartProgressBar()
                }
                it.fraction = currentProgressDone.toDouble() / currentProgressSize.toDouble()
            }
        }
    }

    @Synchronized
    private fun ensureInTransaction() {
        if (inTransaction) return
        inTransaction = true
        rootsManager.startTransaction()
    }

    @Synchronized
    private fun endBatch() {
        check(inTransaction)
        rootsManager.commit()
        inTransaction = false
    }

    private abstract inner class Worker {
        private var shouldStop = false

        open fun start() {
            ensureInTransaction()
        }

        fun stopGracefully() {
            shouldStop = true
        }

        protected open fun checkCancelled() = false
        protected abstract fun close()

        protected fun run() {
            try {
                while (true) {
                    // prevent parallel work in both silent and under progress
                    synchronized(work) {
                        val next = synchronized(this@DefaultBackgroundExecutor) {
                            if (shouldStop) return

                            if (checkCancelled()) {
                                queue.clear()
                                endBatch()
                                return
                            } else if (queue.isEmpty()) {
                                endBatch()
                                return
                            }

                            queue.poll()?.also {
                                currentFile = it.key
                                currentProgressDone++
                                updateProgress()
                            }
                        }

                        next?.actions?.invoke()

                        synchronized(work) {
                            currentFile = null
                        }
                    }
                }
            } finally {
                close()
            }
        }
    }

    private inner class UnderProgressWorker : Worker() {
        var progressIndicator: ProgressIndicator? = null

        override fun start() {
            super.start()

            object : Task.Backgroundable(project, "Kotlin: Loading script dependencies...", true) {
                override fun run(indicator: ProgressIndicator) {
                    progressIndicator = indicator
                    updateProgress()
                    run()
                }
            }.queue()
        }

        override fun checkCancelled(): Boolean = progressIndicator?.isCanceled == true

        override fun close() {
            synchronized(this@DefaultBackgroundExecutor) {
                underProgressWorker = null
                progressIndicator = null
            }
        }
    }

    private inner class SilentWorker : Worker() {
        override fun start() {
            super.start()

            BackgroundTaskUtil.executeOnPooledThread(project, Runnable {
                run()
            })
        }

        override fun close() {
            synchronized(this@DefaultBackgroundExecutor) {
                silentWorker = null
            }
        }
    }
}

