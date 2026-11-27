/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.example.testdatamanager.fakes.FakeOutsideDefaultTestGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestPackageCheck.Companion.testPackageError
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.packagecheck.FakeInheritingTestGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.packagecheck.FakePackageCheckHolder
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.packagecheck.FakePackageCheckTestGenerated
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener

class ManagedTestPackageCheckTest {
    private val fakes = FakePackageCheckTestGenerated::class.java.`package`.name

    private fun outside(testClass: Class<*>, packages: String): String =
        "${testClass.name} is a managed test outside the packages of `testDataManager { testPackages }` of its module " +
                "($packages), so the test data manager doesn't find it: add its package there"

    /** Runs [testClass] with [packages] as the `test` task of a module runs it; returns the passed tests and the failures. */
    private fun run(testClass: Class<*>, packages: String?): Pair<Long, List<String?>> {
        val previous = System.getProperty(TEST_DATA_MANAGER_OPTIONS_TEST_PACKAGES)
        if (packages != null) System.setProperty(TEST_DATA_MANAGER_OPTIONS_TEST_PACKAGES, packages)
        else System.clearProperty(TEST_DATA_MANAGER_OPTIONS_TEST_PACKAGES)
        try {
            val listener = SummaryGeneratingListener()
            val config = LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build()
            val request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(testClass)).build()
            LauncherFactory.create(config).execute(request, listener)
            return listener.summary.testsSucceededCount to listener.summary.failures.map { it.exception.message }
        } finally {
            if (previous != null) System.setProperty(TEST_DATA_MANAGER_OPTIONS_TEST_PACKAGES, previous)
            else System.clearProperty(TEST_DATA_MANAGER_OPTIONS_TEST_PACKAGES)
        }
    }

    @Test
    fun `generated tests run inside the packages, with their nested classes`() {
        assertEquals(2L to emptyList<String>(), run(FakePackageCheckTestGenerated::class.java, "org.example,$fakes"))
        assertEquals(2L to emptyList<String>(), run(FakePackageCheckTestGenerated::class.java, "org.jetbrains.kotlin.analysis"))
    }

    @Test
    fun `generated tests outside the packages fail, with their nested classes`() {
        val packages = "org.example, ${fakes.dropLast(1)}"
        assertEquals(
            0L to listOf(outside(FakePackageCheckTestGenerated::class.java, packages)),
            run(FakePackageCheckTestGenerated::class.java, packages.replace(" ", "")),
        )
    }

    @Test
    fun `a nested generated test that JUnit discovers on its own is checked on its own`() {
        val testClass = FakePackageCheckHolder.StandaloneGenerated::class.java
        assertEquals(0L to listOf(outside(testClass, "org.example")), run(testClass, "org.example"))
    }

    @Test
    fun `a nested test inherited from a class in another package is checked as the class it runs in`() {
        assertEquals(2L to emptyList<String>(), run(FakeInheritingTestGenerated::class.java, fakes))
    }

    @Test
    fun `without the packages, nothing is checked`() {
        val testClass = FakeOutsideDefaultTestGenerated::class.java
        assertEquals(1L to emptyList<String>(), run(testClass, packages = null))
        // Not the default packages of the managed tests either
        assertEquals(0L to listOf(outside(testClass, "org.jetbrains.kotlin")), run(testClass, packages = ""))
    }

    @Test
    fun `other classes are not looked for, and the default package can't be declared`() {
        assertNull(testPackageError(ManagedTestPackageCheckTest::class.java.name, listOf("org.example")))
        assertEquals(
            "FooGenerated is a managed test in the default package, where the test data manager doesn't look for tests: move it " +
                    "into a package of `testDataManager { testPackages }` of its module (org.example)",
            testPackageError("FooGenerated", listOf("org.example")),
        )
    }
}
