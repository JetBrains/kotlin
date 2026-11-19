/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.runner

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.assertEqualsToTestDataFile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test
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

    fun reset(root: Path) {
        this.root = root
        outputs.clear()
        flakyFailures.set(0)
    }

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
}

@TestMetadata("runner")
class FakeRunnerJsTestGenerated : ManagedTest {
    override val variantChain: List<String> = listOf("js")

    @Test
    @TestMetadata("first.kt")
    fun testFirst() = FakeRunnerScenario.assertOutput(this, "js", "first")

    @Test
    @TestMetadata("second.kt")
    fun testSecond() = FakeRunnerScenario.assertOutput(this, "js", "second")
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
