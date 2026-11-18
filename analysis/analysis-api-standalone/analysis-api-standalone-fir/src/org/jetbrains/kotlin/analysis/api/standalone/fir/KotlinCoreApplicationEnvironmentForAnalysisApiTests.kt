/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.analysis.api.standalone.fir

import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment

internal class KotlinCoreApplicationEnvironmentForAnalysisApiTests(parentDisposable: Disposable) : KotlinCoreApplicationEnvironment(
    parentDisposable,
    unitTestMode = true
) {
    override fun createApplication(parentDisposable: Disposable): MockApplication {
        return KotlinCoreUnitTestApplication(parentDisposable)
    }
}

/**
 * A [MockApplication] which allows write actions only in [runWriteAction] blocks.
 *
 * The Analysis API is usually not allowed to be used from a write action. To properly support Analysis API tests, the application should
 * not return `true` for [isWriteAccessAllowed] indiscriminately like [MockApplication]. On the other hand, we have some tests which require
 * write access, but don't access the Analysis API. Hence, we remember which threads have started a write action with [runWriteAction].
 */
private class KotlinCoreUnitTestApplication(parentDisposable: Disposable) : MockApplication(parentDisposable) {
    override fun isUnitTestMode(): Boolean = true

    override fun isWriteAccessAllowed(): Boolean = PlatformWriteAccessSupport.isWriteAccessAllowed()

    override fun runWriteAction(action: Runnable) {
        withWriteAccessAllowedInThread {
            action.run()
        }
    }

    override fun <T> runWriteAction(computation: Computable<T?>): T? =
        withWriteAccessAllowedInThread { computation.compute() }

    override fun <T, E : Throwable?> runWriteAction(computation: ThrowableComputable<T?, E?>): T? =
        withWriteAccessAllowedInThread { computation.compute() }

    private inline fun <A> withWriteAccessAllowedInThread(action: () -> A): A = PlatformWriteAccessSupport.withWriteAccessAllowedInThread(action)
}

/**
 * This object is required because write access may be requested during the entire tree disposal.
 * Thus, if at that moment the application was already disposed, the information about requested write access may be lost.
 */
private object PlatformWriteAccessSupport {
    /**
     * We need to remember whether write access is allowed per thread because the application can be shared between multiple concurrent test
     * runs.
     */
    private val isWriteAccessAllowedInThread: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    fun isWriteAccessAllowed(): Boolean = isWriteAccessAllowedInThread.get()

    inline fun <A> withWriteAccessAllowedInThread(action: () -> A): A {
        isWriteAccessAllowedInThread.set(true)
        try {
            return action()
        } finally {
            isWriteAccessAllowedInThread.set(false)
        }
    }
}
