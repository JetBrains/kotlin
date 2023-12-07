/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.ProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

class KotlinCoreEnvironmentSharedApplicationDisposalTest {
    @Test
    @Timeout(30)
    fun applicationManagerIsResetAfterSharedApplicationDisposal() {
        val projectDisposable1 = Disposer.newDisposable("Disposable for project #1")
        val projectDisposable2 = Disposer.newDisposable("Disposable for project #2")

        try {
            val configuration = DisposalTestApplicationEnvironmentConfiguration()

            val applicationEnvironment1 = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
                projectDisposable1,
                CompilerConfiguration.EMPTY,
                configuration,
            )

            val applicationEnvironment2 = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
                projectDisposable2,
                CompilerConfiguration.EMPTY,
                configuration,
            )

            assertEquals(applicationEnvironment1, applicationEnvironment2) {
                "The shared application environment should be the same for projects #1 and #2."
            }

            val application = applicationEnvironment1.application

            assertEquals(application, ApplicationManager.getApplication()) {
                "The application manager's application should be equal to the application environment's application."
            }

            Disposer.dispose(projectDisposable1)

            assertEquals(application, ApplicationManager.getApplication()) {
                "The application manager's application should not have been reset yet, as only project #1 was disposed."
            }

            // Ensure that no other test can request a new shared application environment between project disposal and the application
            // manager check.
            synchronized(KotlinCoreEnvironment.APPLICATION_LOCK) {
                Disposer.dispose(projectDisposable2)

                // This test might fail on two occasions:
                //  1. `KotlinCoreEnvironment` doesn't properly reset the application. This is what we want to test.
                //  2. Another test might have already registered a new non-shared application environment. However, this is a contract
                //     violation, so it's good to catch it.
                assertNull(ApplicationManager.getApplication()) {
                    "The application manager's application should have been reset to `null` after the last project was disposed."
                }
            }
        } finally {
            // If the test fails, we still need to properly clean up our project disposables.
            Disposer.dispose(projectDisposable1, /* processUnregistered = */ false)
            Disposer.dispose(projectDisposable2, /* processUnregistered = */ false)
        }
    }

    /**
     * The disposal of the last active project triggers disposal of the application environment. The shared application environment must be
     * disposed at exactly the right time: when application and project services have already been disposed. Otherwise, services might
     * encounter effects such as [ApplicationManager.getApplication] returning `null`.
     */
    @Test
    fun applicationServicesAreDisposedBeforeTheirSharedApplicationEnvironment() {
        val projectDisposable = Disposer.newDisposable("Project disposable")

        try {
            val configuration = DisposalTestApplicationEnvironmentConfiguration()

            val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
                projectDisposable,
                CompilerConfiguration.EMPTY,
                configuration,
            )

            val application = applicationEnvironment.application

            application.registerService(CheckApplicationDuringDisposalService::class.java)
            application.getService(CheckApplicationDuringDisposalService::class.java)

            Disposer.dispose(projectDisposable)
        } finally {
            // If the test fails, we still need to properly clean up our project disposable.
            Disposer.dispose(projectDisposable, /* processUnregistered = */ false)
        }
    }

    private class CheckApplicationDuringDisposalService() : Disposable {
        override fun dispose() {
            assertNotNull(ApplicationManager.getApplication())
        }
    }

    /**
     * @see applicationServicesAreDisposedBeforeTheirSharedApplicationEnvironment
     */
    @Test
    fun projectServicesAreDisposedBeforeTheirSharedApplicationEnvironment() {
        val disposable = Disposer.newDisposable("Disposable for shared environment")

        try {
            val configuration = DisposalTestApplicationEnvironmentConfiguration()

            val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
                disposable,
                CompilerConfiguration.EMPTY,
                configuration,
            )

            val projectEnvironment = ProjectEnvironment(disposable, applicationEnvironment, CompilerConfiguration.EMPTY)
            val project = projectEnvironment.project

            project.registerService(CheckApplicationDuringDisposalService::class.java)
            project.getService(CheckApplicationDuringDisposalService::class.java)

            Disposer.dispose(disposable)
        } finally {
            // If the test fails, we still need to properly clean up our project disposable.
            Disposer.dispose(disposable, /* processUnregistered = */ false)
        }
    }
}

/**
 * Our tests need to use a configuration with a unique identity, because with a default configuration, another test might interfere by
 * keeping the shared application environment alive, or our tests might interfere with other tests by doing something strange with the
 * application environment.
 */
private class DisposalTestApplicationEnvironmentConfiguration : KotlinCoreApplicationEnvironmentConfiguration {
    override val isUnitTestMode: Boolean get() = true
    override val configurators: List<KotlinCoreApplicationEnvironmentConfigurator> get() = emptyList()
}
