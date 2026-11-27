/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class TestFailureSuppressor(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    /**
     * This function should be used to suppress test failures if there were any.
     * It's guaranteed that the [failedAssertions] list will not be empty.
     *
     * A suppressor which suppresses some failures as expected ones may also tell them in advance, see [isExpectedFailure].
     */
    abstract fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException>

    /**
     * This function should be used to check if the test is muted by some means (e.g., using ignore directives),
     * but the test actually passed. In such a situation the assertion error about test unmuting should be thrown.
     *
     * It's guaranteed that this function is called only when no exceptions were thrown during the test.
     */
    abstract fun checkIfTestShouldBeUnmuted()

    /**
     * Whether the test declares failures this suppressor expects, e.g., by an ignore directive or a `.fail` file, so its output may be
     * wrong. Other failures of such a test may still be unexpected, e.g., a backend crash in a test whose `.fail` file is for a frontend one.
     */
    open fun expectsFailures(): Boolean = false

    /**
     * Whether [suppressIfNeeded] would suppress [failure] as an expected one, e.g., a failure in a phase after the one the test runs till.
     * The test runner asks it before the handlers compare their output, to tell an expected failure from a crash which leaves the output
     * incomplete, while [suppressIfNeeded] runs after all checks and may have side effects, e.g., propose edits of the test data. An
     * override must agree with [suppressIfNeeded], which stays the function to implement.
     */
    open fun isExpectedFailure(failure: WrappedException): Boolean = false

    typealias Order = AfterAnalysisChecker.Order

    open val order: Order
        get() = Order.P3

    protected fun Throwable.wrap(): WrappedException = WrappedException.FromAfterAnalysisChecker(this)
}

abstract class SimpleTestFailureSuppressor(testServices: TestServices) : TestFailureSuppressor(testServices) {
    abstract fun testIsMuted(): Boolean

    override fun expectsFailures(): Boolean = testIsMuted()

    final override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        return if (testIsMuted()) emptyList() else failedAssertions
    }
}

abstract class TestFailureSuppressorBySingleDirective(
    val suppressDirective: Directive,
    directivesContainer: DirectivesContainer,
    testServices: TestServices,
    final override val order: Order = Order.P3,
) : SimpleTestFailureSuppressor(testServices) {
    init {
        checkTestInfrastructure(suppressDirective in directivesContainer) { "Directive $suppressDirective not found in directives container" }
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(directivesContainer)

    override fun checkIfTestShouldBeUnmuted() {
        if (testIsMuted()) {
            throw AssertionError("Test contains $suppressDirective directive but no errors was reported. Please remove the directive")
        }
    }

    override fun testIsMuted(): Boolean = suppressDirective in testServices.moduleStructure.allDirectives
}
