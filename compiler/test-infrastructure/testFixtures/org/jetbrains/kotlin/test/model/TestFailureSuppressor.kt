/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class TestFailureSuppressor(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    abstract fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException>

    typealias Order = AfterAnalysisChecker.Order

    open val order: Order
        get() = Order.P3

    protected fun Throwable.wrap(): WrappedException = WrappedException.FromAfterAnalysisChecker(this)
}

abstract class TestFailureSuppressorBySingleDirective(
    val suppressDirective: Directive,
    directivesContainer: DirectivesContainer,
    testServices: TestServices,
    final override val order: Order = Order.P3,
) : TestFailureSuppressor(testServices) {
    init {
        require(suppressDirective in directivesContainer)
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(directivesContainer)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isDisabled()) {
            return failedAssertions
        }

        return if (failedAssertions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $suppressDirective directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private fun isDisabled(): Boolean = suppressDirective in testServices.moduleStructure.allDirectives
}
