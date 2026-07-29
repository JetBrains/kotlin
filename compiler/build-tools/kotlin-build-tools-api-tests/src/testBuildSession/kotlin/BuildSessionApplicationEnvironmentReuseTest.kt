/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2PlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectWithPolicyCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests that the shared ApplicationEnvironment is kept alive within the BuildSession lifetime.
 *
 * Disposal is observed directly through IntelliJ's `Disposer`: a child observer is
 * registered on the shared application environment's `parentDisposable`, so its callback fires exactly when — and
 * each time — the environment is actually disposed.
 */
class BuildSessionApplicationEnvironmentReuseTest : BaseCompilationTest() {
    private var previousKeepalive: String? = null

    @BeforeEach
    fun disableKeepalive() {
        // ensure keepalive is disabled to observe disposals
        previousKeepalive = System.getProperty(KEEPALIVE_PROPERTY)
        System.clearProperty(KEEPALIVE_PROPERTY)
    }

    @AfterEach
    fun restoreKeepalive() {
        val previous = previousKeepalive
        if (previous == null) {
            System.clearProperty(KEEPALIVE_PROPERTY)
        } else {
            System.setProperty(KEEPALIVE_PROPERTY, previous)
        }
    }

    @DisplayName("Multiple compilations within same session should NOT dispose ApplicationEnvironment")
    @BtaV2PlatformAgnosticCompilationTest
    fun multipleCompilationsInSameSessionReuseEnvironment(projectCreator: ProjectWithPolicyCreator, toolchains: KotlinToolchains) {
        val project = projectCreator.inProcess(toolchains)
        lateinit var disposalCount: AtomicInteger
        project {
            val module = module(MODULE_A)
            module.compile() // the first in-process compilation creates and pins the shared environment

            // The environment now exists: start observing its disposal.
            disposalCount = EnvironmentDisposal.observeCurrentEnvironment()

            module.compile()
            module.compile()

            assertEquals(
                0, disposalCount.get(),
                "Expected no ApplicationEnvironment disposal while the session is still open"
            )
        }

        assertEquals(
            1, disposalCount.get(),
            "Expected exactly one ApplicationEnvironment disposal at session end"
        )
    }

    @DisplayName("ApplicationEnvironment should be disposed when BuildSession ends and new one created")
    @BtaV2PlatformAgnosticCompilationTest
    fun environmentDisposedBetweenSessions(projectCreator: ProjectWithPolicyCreator, toolchains: KotlinToolchains) {
        val project = projectCreator.inProcess(toolchains)
        lateinit var disposalCount1: AtomicInteger
        project {
            module(MODULE_A).compile()
            disposalCount1 = EnvironmentDisposal.observeCurrentEnvironment()
            assertEquals(
                0, disposalCount1.get(),
                "Expected no ApplicationEnvironment disposal while the first session is still open"
            )
        }

        assertEquals(
            1, disposalCount1.get(),
            "Expected exactly one ApplicationEnvironment disposal when BuildSession ends"
        )

        lateinit var disposalCount2: AtomicInteger
        project {
            // A different module than the first session, so the two sessions don't share a module directory.
            module(MODULE_B).compile()

            // The second session created a fresh environment (and a fresh parentDisposable); observe it separately.
            disposalCount2 = EnvironmentDisposal.observeCurrentEnvironment()
            assertEquals(
                0, disposalCount2.get(),
                "Expected no ApplicationEnvironment disposal while the second session is still open"
            )
        }

        assertEquals(
            1, disposalCount2.get(),
            "Expected exactly one ApplicationEnvironment disposal when BuildSession ends"
        )
    }

    @DisplayName("Overlapping sessions share one ApplicationEnvironment, disposed only when the last one ends")
    @BtaV2PlatformAgnosticCompilationTest
    fun environmentSharedAcrossOverlappingSessions(projectCreator: ProjectWithPolicyCreator, toolchains: KotlinToolchains) {
        val project = projectCreator.inProcess(toolchains)
        lateinit var disposalCount: AtomicInteger
        project {
            module(MODULE_A).compile()

            // The environment now exists; observe the single shared instance both sessions pin.
            disposalCount = EnvironmentDisposal.observeCurrentEnvironment()

            // A nested session whose lifetime overlaps this one; it uses a different module so the two sessions
            // don't share a module directory. The nested project closes its session first.
            this@BuildSessionApplicationEnvironmentReuseTest.project {
                module(MODULE_B).compile()
            }

            // The first session has ended, but the second still pins the shared environment: it must stay alive.
            assertEquals(
                0, disposalCount.get(),
                "Expected the shared environment to stay alive while the second session is still open"
            )
        }

        // Both sessions have ended: the shared environment must be disposed exactly once.
        assertEquals(
            1, disposalCount.get(),
            "Expected the shared environment to be disposed once the last session ends"
        )
    }

    private fun ProjectWithPolicyCreator.inProcess(toolchains: KotlinToolchains): ProjectCreator {
        val inProcess = toolchains.createInProcessExecutionPolicy()
        return { action -> this@inProcess(this, inProcess, action) }
    }

    /**
     * Observes disposal of the shared `ApplicationEnvironment` that BTA keeps
     * alive for the duration of a [KotlinToolchains.BuildSession].
     *
     * The implementation runs in the isolated [btaClassloader], so the compiler's static environment state and the
     * IntelliJ `Disposer`/`Disposable` types are not the ones on this test's own class path. Everything here
     * is therefore resolved reflectively: the environment state comes from the compiler [Companion][companionInstance],
     * and the `Disposable`/`Disposer` types are recovered — with their real relocated names and class loader — from
     * the signature of the compiler's own `getOrCreateApplicationEnvironmentForProduction` method.
     */
    private object EnvironmentDisposal {
        private val kotlinCoreEnvironmentClass: Class<*> =
            btaClassloader.loadClass("org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment")

        // The `Companion` object holds the shared-environment state and the public `applicationEnvironment` accessor.
        private val companionInstance: Any =
            kotlinCoreEnvironmentClass.getField("Companion").get(null)

        private val getApplicationEnvironment =
            companionInstance.javaClass.getMethod("getApplicationEnvironment")

        /**
         * The IntelliJ `Disposable` interface, taken from the first parameter of the compiler's
         * `getOrCreateApplicationEnvironmentForProduction(Disposable, CompilerConfiguration)` — this yields the exact
         * class, loaded by the exact class loader the compiler uses.
         * Used to derive Disposer class regardless of the package relocation to not hardcode relocated FQN.
         */
        private val disposableInterface: Class<*> =
            companionInstance.javaClass.methods
                .first { it.name == "getOrCreateApplicationEnvironmentForProduction" }
                .parameterTypes[0]

        private val disposerRegister =
            disposableInterface.classLoader
                .loadClass(disposableInterface.name.removeSuffix("Disposable") + "util.Disposer")
                .getMethod("register", disposableInterface, disposableInterface)

        private fun currentApplicationEnvironment(): Any? = getApplicationEnvironment.invoke(companionInstance)

        /**
         * Registers a disposal observer on the currently alive shared application environment and returns a counter
         * that is incremented each time that specific environment is disposed. Registering a child on the
         * environment's `parentDisposable` does not affect the environment's reference count, so it does not keep the
         * environment alive.
         */
        fun observeCurrentEnvironment(): AtomicInteger {
            val applicationEnvironment = currentApplicationEnvironment()
                ?: error("Expected a live application environment to observe, but none has been created yet")

            val parentDisposable = applicationEnvironment.javaClass
                .getMethod("getParentDisposable").invoke(applicationEnvironment)

            val disposalCount = AtomicInteger(0)
            val observer = Proxy.newProxyInstance(
                disposableInterface.classLoader, arrayOf(disposableInterface)
            ) { proxy, method, args ->
                when (method.name) {
                    "dispose" -> {
                        disposalCount.incrementAndGet()
                        null
                    }
                    "equals" -> proxy === args?.getOrNull(0)
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "EnvironmentDisposalObserver@${System.identityHashCode(proxy)}"
                    else -> null
                }
            }
            disposerRegister.invoke(null, parentDisposable, observer)
            return disposalCount
        }
    }

    private companion object {
        const val KEEPALIVE_PROPERTY = "kotlin.environment.keepalive"

        const val MODULE_A = "basic-multimodule-project/module-1"
        const val MODULE_B = "basic-multimodule-project/module-3"
    }
}
