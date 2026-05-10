/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class TestFailureSuppressor(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    /**
     * This function should be used to suppress test failures if there were any.
     * It's guaranteed that the [failedAssertions] list will not be empty.
     */
    abstract fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException>

    /**
     * This function should be used to check if the test is muted by some means (e.g., using ignore directives),
     * but the test actually passed. In such a situation the assertion error about test unmuting should be thrown.
     *
     * It's guaranteed that this function is called only when no exceptions were thrown during the test.
     */
    abstract fun checkIfTestShouldBeUnmuted()

    typealias Order = AfterAnalysisChecker.Order

    open val order: Order
        get() = Order.P3

    protected fun Throwable.wrap(): WrappedException = WrappedException.FromAfterAnalysisChecker(this)
}

abstract class SimpleTestFailureSuppressor(testServices: TestServices) : TestFailureSuppressor(testServices) {
    abstract fun testIsMuted(): Boolean

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
        require(suppressDirective in directivesContainer)
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(directivesContainer)

    override fun checkIfTestShouldBeUnmuted() {
        if (testIsMuted()) {
            throw AssertionError("Test contains $suppressDirective directive but no errors was reported. Please remove the directive")
        }
    }

    override fun testIsMuted(): Boolean = suppressDirective in testServices.moduleStructure.allDirectives
}
