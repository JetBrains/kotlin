/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility for postponing indexing of new roots to the end of some bulk operation.
 */
class ScriptClassRootsIndexer(
    val project: Project,
    val manager: CompositeScriptConfigurationManager
) {
    private var invalidated: Boolean = false
    private val concurrentUpdates = AtomicInteger()

    @Synchronized
    fun invalidate() {
        checkInTransaction()
        invalidated = true
    }

    fun checkInTransaction() {
        check(concurrentUpdates.get() > 0)
    }

    inline fun <T> update(body: () -> T): T {
        startTransaction()
        return try {
            body()
        } finally {
            commit()
        }
    }

    fun startTransaction() {
        concurrentUpdates.incrementAndGet()
    }

    fun commit() {
        concurrentUpdates.decrementAndGet()

        // run indexing even in inner transaction
        // (outer transaction may be async, so it would be better to not wait it)
        startIndexingIfNeeded()
    }

    @Synchronized
    private fun startIndexingIfNeeded() {
        if (!invalidated) return
        invalidated = false

        if (manager.collectRootsAndCheckNew()) {
            val doNotifyRootsChanged = Runnable {
                runWriteAction {
                    if (project.isDisposed) return@runWriteAction

                    debug { "roots change event" }

                    ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                    ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
                }
            }

            if (ApplicationManager.getApplication().isUnitTestMode) {
                TransactionGuard.submitTransaction(project, doNotifyRootsChanged)
            } else {
                TransactionGuard.getInstance().submitTransactionLater(project, doNotifyRootsChanged)
            }
        }
    }
}