/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.DiscoveredTest
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.FakeManagedTest
import org.junit.platform.engine.Filter
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherFactory

/**
 * Base class for integration tests that use fake test classes.
 *
 * Provides helper methods to discover tests from the fake test package.
 */
internal abstract class AbstractFakeTestIntegrationTest {
    protected fun buildFakeDiscoveryRequest(
        testClassPattern: String? = null,
        testDataPath: String? = null,
        vararg additionalFilters: Filter<*>,
    ): LauncherDiscoveryRequest = TestDataManagerRunner.buildDiscoveryRequest(
        testClassPattern,
        testDataPath,
        PackageNameFilter.includePackageNames(FakeManagedTest::class.java.`package`.name),
        *additionalFilters,
    )

    protected fun discoverFakeTestPlan(
        testClassPattern: String? = null,
        testDataPath: String? = null,
        vararg additionalFilters: Filter<*>,
    ): TestPlan {
        val request = buildFakeDiscoveryRequest(testClassPattern, testDataPath, *additionalFilters)
        return LauncherFactory.create().discover(request)
    }

    protected fun discoverFakeTests(
        testClassPattern: String? = null,
        testDataPath: String? = null,
        vararg additionalFilters: Filter<*>,
    ): List<DiscoveredTest> {
        val testPlan = discoverFakeTestPlan(testClassPattern, testDataPath, *additionalFilters)
        return TestDataManagerRunner.discoverTests(testPlan)
    }

    protected fun <T> discoverFakeTests(
        testClassPattern: String? = null,
        testDataPath: String? = null,
        vararg additionalFilters: Filter<*>,
        transform: (TestIdentifier) -> T,
    ): List<T> {
        val testPlan = discoverFakeTestPlan(testClassPattern, testDataPath, *additionalFilters)
        return TestDataManagerRunner.discoverTests(testPlan, transform)
    }
}
