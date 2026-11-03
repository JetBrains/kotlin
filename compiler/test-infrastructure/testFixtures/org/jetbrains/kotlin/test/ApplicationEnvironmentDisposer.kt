/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.services.ApplicationDisposableProvider
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class ApplicationEnvironmentDisposer : TestExecutionListener {
    companion object {
        val ROOT_DISPOSABLE: Disposable = Disposer.newDisposable("${ApplicationEnvironmentDisposer::class.simpleName}.ROOT_DISPOSABLE")
    }

    @OptIn(CoreEnvironmentDeprecation::class)
    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        KotlinCoreEnvironment.disposeApplicationEnvironment()
        Disposer.dispose(ROOT_DISPOSABLE)
        KotlinCoreEnvironment.resetApplicationManager()
    }
}

class ExecutionListenerBasedDisposableProvider : ApplicationDisposableProvider() {
    override fun getApplicationRootDisposable(): Disposable {
        return ApplicationEnvironmentDisposer.ROOT_DISPOSABLE
    }
}

/**
 * Keeps the shared application environment alive until [parentDisposable] is disposed.
 *
 * `KotlinCoreEnvironment` counts live projects and disposes the environment when the count drops to zero, see
 * `KotlinCoreEnvironment.getOrCreateApplicationEnvironment`. The pin counts as one more project, so the environment
 * survives while test projects come and go. `KotlinCoreEnvironment.disposeApplicationEnvironment()` still disposes it.
 *
 * The pin is not related to [ApplicationEnvironmentDisposer.ROOT_DISPOSABLE]. That disposable never holds the
 * environment. It only owns caches and services which live until the end of the test plan.
 *
 * The pin creates the environment in the unit-test mode, like the test infrastructure does. The first creator fixes the
 * mode for as long as the environment lives.
 */
@OptIn(CoreEnvironmentDeprecation::class)
fun pinApplicationEnvironment(parentDisposable: Disposable) {
    KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(parentDisposable, CompilerConfiguration.create())
}
