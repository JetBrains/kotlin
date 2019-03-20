/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry

/**
 * Temporary allow resolve in write action.
 *
 * All resolve should be banned from write action. This method is needed for the transition period to document
 * places that are not fixed yet.
 */
fun <T> allowResolveInWriteAction(runnable: () -> T): T {
    return ResolveInWriteActionManager.runWithResolveAllowedInWriteAction(runnable)
}

internal object ResolveInWriteActionManager {
    private val LOG = Logger.getInstance(ResolveInWriteActionManager::class.java)

    private val isResolveInWriteActionAllowed: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    fun assertNoResolveUnderWriteAction() {
        if (isResolveInWriteActionAllowed.get()) return

        val application = ApplicationManager.getApplication() ?: return

        if (!application.isWriteAccessAllowed) return

        if (application.isUnitTestMode) return

        @Suppress("InvalidBundleOrProperty")
        if (!Registry.`is`("kotlin.write.resolve.check", false)) return

        LOG.error("Resolve is not allowed under the write action!")
    }

    inline fun <T> runWithResolveAllowedInWriteAction(runnable: () -> T): T {
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
}