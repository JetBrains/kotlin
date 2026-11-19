/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.runner

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.assertEqualsToTestDataFile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake managed suites that the runner executes in `TestDataManagerRunnerIntegrationTest`, configured by [FakeRunnerScenario].
 *
 * Unlike the other fakes, which are only discovered, they run, so the discovery tests exclude this package.
 */
object FakeRunnerScenario {
    /** The directory the `@TestMetadata` paths of the suites are resolved against. */
    lateinit var root: Path

    /** The output of each test, by `<suite label>:<test name>` or by `<test name>` for all suites; none means no output. */
    val outputs: MutableMap<String, String> = ConcurrentHashMap()

    /** How many more times `testFlaky` fails before it passes. */
    val flakyFailures: AtomicInteger = AtomicInteger()

    /** How many more times the class setup of the `[js]` suite fails before it passes. */
    val classSetupFailures: AtomicInteger = AtomicInteger()

    /** How many more times the class setup of the `[js]` suite aborts, once it no longer fails, before it passes. */
    val classSetupAborts: AtomicInteger = AtomicInteger()

    /** How many more times `testFirst` of the `[js]` suite aborts before it runs. */
    val jsFirstAborts: AtomicInteger = AtomicInteger()

    /** How many more times the class teardown of the `[js]` suite fails before it passes. */
    val classTeardownFailures: AtomicInteger = AtomicInteger()

    /** Whether `testSecond` of the `[js]` suite is skipped by its execution condition. */
    @Volatile
    var jsSecondDisabled: Boolean = false

    /**
     * Whether the `[js]` suite can't be instantiated once the golden suite started, so after the discovery, which
     * instantiates it to read the variant chain.
     */
    @Volatile
    var jsConstructionFailsAfterGolden: Boolean = false

    @Volatile
    var goldenSuiteStarted: Boolean = false

    fun reset(root: Path) {
        this.root = root
        outputs.clear()
        flakyFailures.set(0)
        classSetupFailures.set(0)
        classSetupAborts.set(0)
        jsFirstAborts.set(0)
        classTeardownFailures.set(0)
        jsSecondDisabled = false
        jsConstructionFailsAfterGolden = false
        goldenSuiteStarted = false
    }

    @JvmStatic
    fun isJsSecondDisabled(): Boolean = jsSecondDisabled

    internal fun assertOutput(test: ManagedTest, label: String, name: String) {
        test.assertEqualsToTestDataFile(
            testDataPath = root.resolve("runner/$name.kt"),
            actual = outputs["$label:$name"] ?: outputs[name],
            extension = ".txt",
        )
    }
}

@TestMetadata("runner")
class FakeRunnerGoldenTestGenerated : ManagedTest {
    @Test
    @TestMetadata("first.kt")
    fun testFirst() = FakeRunnerScenario.assertOutput(this, "golden", "first")

    @Test
    @TestMetadata("second.kt")
    fun testSecond() = FakeRunnerScenario.assertOutput(this, "golden", "second")

    @Test
    @TestMetadata("flaky.kt")
    fun testFlaky() {
        check(FakeRunnerScenario.flakyFailures.getAndDecrement() <= 0) { "Flaky failure" }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpClass() {
            FakeRunnerScenario.goldenSuiteStarted = true
        }
    }
}

@TestMetadata("runner")
class FakeRunnerJsTestGenerated : ManagedTest {
    init {
        check(!FakeRunnerScenario.jsConstructionFailsAfterGolden || !FakeRunnerScenario.goldenSuiteStarted) { "Construction failure" }
    }

    override val variantChain: List<String> = listOf("js")

    @Test
    @TestMetadata("first.kt")
    fun testFirst() {
        assumeTrue(FakeRunnerScenario.jsFirstAborts.getAndDecrement() <= 0, "Test abort")
        FakeRunnerScenario.assertOutput(this, "js", "first")
    }

    @Test
    @TestMetadata("second.kt")
    @DisabledIf("org.jetbrains.kotlin.analysis.test.data.manager.fakes.runner.FakeRunnerScenario#isJsSecondDisabled")
    fun testSecond() = FakeRunnerScenario.assertOutput(this, "js", "second")

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpClass() {
            check(FakeRunnerScenario.classSetupFailures.getAndDecrement() <= 0) { "Class setup failure" }
            assumeTrue(FakeRunnerScenario.classSetupAborts.getAndDecrement() <= 0, "Class setup abort")
        }

        @JvmStatic
        @AfterAll
        fun tearDownClass() {
            check(FakeRunnerScenario.classTeardownFailures.getAndDecrement() <= 0) { "Class teardown failure" }
        }
    }
}

@TestMetadata("runner")
class FakeRunnerKnmJsTestGenerated : ManagedTest {
    override val variantChain: List<String> = listOf("knm", "js")

    @Test
    @TestMetadata("first.kt")
    fun testFirst() = FakeRunnerScenario.assertOutput(this, "knmJs", "first")
}

/** A suite without `@TestMetadata`, like hand-written ones. */
class FakeRunnerNoMetadataTestGenerated : ManagedTest {
    @Test
    fun testWithoutMetadata() = FakeRunnerScenario.assertOutput(this, "noMetadata", "noMetadata")
}
