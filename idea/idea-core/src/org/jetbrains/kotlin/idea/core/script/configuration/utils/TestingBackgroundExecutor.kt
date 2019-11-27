/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.HashSetQueue

class TestingBackgroundExecutor internal constructor(
    private val rootsManager: ScriptClassRootsIndexer
) : BackgroundExecutor {
    val backgroundQueue = HashSetQueue<BackgroundTask>()

    class BackgroundTask(val file: VirtualFile, val actions: () -> Unit) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BackgroundTask

            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            return file.hashCode()
        }
    }

    @Synchronized
    override fun ensureScheduled(key: VirtualFile, actions: () -> Unit) {
        backgroundQueue.add(BackgroundTask(key, actions))
    }

    fun doAllBackgroundTaskWith(actions: () -> Unit): Boolean {
        val copy = backgroundQueue.toList()
        backgroundQueue.clear()

        actions()

        rootsManager.transaction {
            copy.forEach {
                it.actions()
            }
        }

        LaterInvocator.ensureFlushRequested()
        LaterInvocator.dispatchPendingFlushes()

        return copy.isNotEmpty()
    }
}