/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.TestOnly

/**
 * Temporary allow resolve in write action.
 *
 * All resolve should be banned from write action. This method is needed for the transition period to document
 * places that are not fixed yet.
 */
fun <T> allowResolveInWriteAction(runnable: () -> T): T {
    return ResolveInWriteActionManager.runWithResolveAllowedInWriteAction(runnable)
}

/**
 * Force resolve check in tests.
 */
@TestOnly
fun <T> forceResolveInWriteActionCheckInTests(errorHandler: (() -> Unit)? = null, runnable: () -> T): T {
    return ResolveInWriteActionManager.runWithForceResolveAllowedCheckInTests(errorHandler, runnable)
}

private const val RESOLVE_IN_WRITE_ACTION_ERROR_MESSAGE = "Resolve is not allowed under the write action!"

class ResolveInWriteActionException(message: String? = null) : IllegalThreadStateException(message ?: RESOLVE_IN_WRITE_ACTION_ERROR_MESSAGE)

internal object ResolveInWriteActionManager {
    private val LOG = Logger.getInstance(ResolveInWriteActionManager::class.java)

    private val isResolveInWriteActionAllowed: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    private val isForceCheckInTests: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    private val errorHandler: ThreadLocal<(() -> Unit)?> = ThreadLocal.withInitial { null }

    fun assertNoResolveUnderWriteAction() {
        if (isResolveInWriteActionAllowed.get()) return

        val application = ApplicationManager.getApplication() ?: return

        if (!application.isWriteAccessAllowed) return

        if (application.isUnitTestMode) {
            if (!isForceCheckInTests.get()) return

            val handler = errorHandler.get()
            if (handler != null) {
                handler()
                return
            }

            throw ResolveInWriteActionException()
        } else {
            @Suppress("InvalidBundleOrProperty")
            if (!Registry.`is`("kotlin.write.resolve.check", false)) return

            LOG.error(RESOLVE_IN_WRITE_ACTION_ERROR_MESSAGE)
        }
    }

    internal inline fun <T> runWithResolveAllowedInWriteAction(runnable: () -> T): T {
        val wasSet =
            if (ApplicationManager.getApplication()?.isWriteAccessAllowed == true && isResolveInWriteActionAllowed.get() == false) {
                isResolveInWriteActionAllowed.set(true)
                true
            } else {
                false
            }

        try {
            return runnable()
        } finally {
            if (wasSet) {
                isResolveInWriteActionAllowed.set(false)
            }
        }
    }

    internal fun <T> runWithForceResolveAllowedCheckInTests(errorHandler: (() -> Unit)?, runnable: () -> T): T {
        val wasSet = if (isForceCheckInTests.get() == false) {
            isForceCheckInTests.set(true)
            this.errorHandler.set(errorHandler)
            true
        } else {
            false
        }

        try {
            return runnable()
        } finally {
            if (wasSet) {
                isForceCheckInTests.set(false)
                this.errorHandler.set(null)
            }
        }
    }
}