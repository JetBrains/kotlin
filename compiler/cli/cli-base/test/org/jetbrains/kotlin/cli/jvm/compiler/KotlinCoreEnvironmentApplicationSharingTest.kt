/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * This test ensures that [KotlinCoreEnvironment.getOrCreateApplicationEnvironment] keeps a single shared application environment for one
 * application environment configuration at a time. Requests with different application environment configurations must wait until the
 * current shared application environment is no longer used.
 *
 * The test works by registering an ID service with the application. It then checks if the application returned by the core environment has
 * the same ID in its [ApplicationEnvironmentIdService]. The test runs many application environment requests in parallel to simulate a
 * concurrent environment.
 */
class KotlinCoreEnvironmentApplicationSharingTest {
    @Test
    @Timeout(30)
    @OptIn(ObsoleteCoroutinesApi::class)
    fun applicationSharingRespectsEnvironmentApplicationConfigurations() {
        val threadPoolContext = newFixedThreadPoolContext(100, "Thread pool context for application sharing test")

        runBlocking(threadPoolContext) {
            coroutineScope {
                val jobs = (1..1000).map { index ->
                    launch { getOrCreateApplicationWithRandomEnvironmentConfiguration(index) }
                }
                jobs.forEach { it.join() }
            }
        }

        threadPoolContext.close()
    }

    private fun getOrCreateApplicationWithRandomEnvironmentConfiguration(index: Int) {
        // The purpose of the disposable is to signal to `KotlinCoreEnvironment` that the application environment is currently in
        // use. This allows us to control the exact lifetime of the application.
        val disposable = Disposer.newDisposable(
            "Disposable for `${KotlinCoreEnvironmentApplicationSharingTest::class.simpleName}`, coroutine #$index"
        )

        try {
            val configuration = getRandomApplicationEnvironmentConfiguration()

            val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
                disposable,
                CompilerConfiguration.EMPTY,
                configuration,
            )

            val currentApplication = ApplicationManager.getApplication()
            assertEquals(applicationEnvironment.application, currentApplication) {
                """
                    The application registered with `ApplicationManager` does not correspond to the application environment's application. 
                    During the lifetime of `disposable`, the application manager's application should correspond to the application 
                    environment's application.
                """.trimIndentToSingleLine()
            }

            val idService = applicationEnvironment.application.getService(ApplicationEnvironmentIdService::class.java)
            assertEquals(configuration.id, idService.id) {
                """
                    The returned application environment does not correspond to its configuration, as the configuration's ID and the 
                    registered application service's ID differ. This means that `getOrCreateApplicationEnvironment` has illegally shared an 
                    application environment with a different configuration than requested.
                """.trimIndentToSingleLine()
            }

            assertEquals(currentApplication, ApplicationManager.getApplication()) {
                """
                    The application returned by `ApplicationManager` is not consistent with the application at the start of the test, but it 
                    should be consistent as long as we haven't disposed the `disposable` yet.
                """.trimIndentToSingleLine()
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }
}

private fun String.trimIndentToSingleLine() = trimIndent().replace('\n', ' ')

private fun getRandomApplicationEnvironmentConfiguration(): ApplicationEnvironmentConfigurationWithId =
    when (Random.nextInt(1..5)) {
        1 -> ApplicationEnvironmentConfiguration1
        2 -> ApplicationEnvironmentConfiguration2
        3 -> ApplicationEnvironmentConfiguration3
        4 -> ApplicationEnvironmentConfiguration4
        5 -> ApplicationEnvironmentConfiguration5
        else -> error("The random integer should be between 1 and 5.")
    }

// We could just instantiate different `ApplicationEnvironmentConfigurationWithId`s and provide a proper `equals` function, but we
// specifically want to test the idiomatic case where application environment configurations are differentiated by object identity.
private object ApplicationEnvironmentConfiguration1 : ApplicationEnvironmentConfigurationWithId(1)
private object ApplicationEnvironmentConfiguration2 : ApplicationEnvironmentConfigurationWithId(2)
private object ApplicationEnvironmentConfiguration3 : ApplicationEnvironmentConfigurationWithId(3)
private object ApplicationEnvironmentConfiguration4 : ApplicationEnvironmentConfigurationWithId(4)
private object ApplicationEnvironmentConfiguration5 : ApplicationEnvironmentConfigurationWithId(5)

private abstract class ApplicationEnvironmentConfigurationWithId(val id: Int) : KotlinCoreApplicationEnvironmentConfiguration {
    override val isUnitTestMode: Boolean get() = true

    override val configurators: List<KotlinCoreApplicationEnvironmentConfigurator> = listOf(ApplicationEnvironmentIdConfigurator(id))

    override fun toString(): String = "ApplicationEnvironmentConfigurationWithId(id = $id)"
}

private class ApplicationEnvironmentIdConfigurator(val id: Int) : KotlinCoreApplicationEnvironmentConfigurator {
    override fun configure(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        applicationEnvironment.application.registerService(ApplicationEnvironmentIdService::class.java, ApplicationEnvironmentIdService(id))
    }
}

private class ApplicationEnvironmentIdService(val id: Int)
