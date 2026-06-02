/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestModule

sealed class WrappedException(
    cause: Throwable,
    val priority: Int,
    val additionalPriority: Int,
) : Exception(cause), Comparable<WrappedException> {
    /**
     * If [failureDisablesNextSteps] is `true`,
     * then the following test steps might not be run considering this exception as critical.
     * If false, the next steps will ignore this exception and continue running.
     */
    open val failureDisablesNextSteps: Boolean get() = true

    abstract val failedModule: TestModule?

    class FromFacade(
        cause: Throwable,
        override val failedModule: TestModule,
        val facade: AbstractTestFacade<*, *>,
    ) : WrappedException(cause, 0, 1) {
        override val message: String
            get() = "Exception was thrown"

        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromFacade(newCause, failedModule, facade)
        }
    }

    class FromGroupingFacade(
        cause: Throwable,
        val facade: AbstractGroupingStageTestFacade<*, *>,
    ) : WrappedException(cause, 0, 1) {
        override val failedModule: TestModule?
            get() = null

        override val message: String
            get() = "Exception was thrown"

        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromGroupingFacade(newCause, facade)
        }
    }

    class FromHandler(
        cause: Throwable,
        override val failedModule: TestModule?,
        val handler: AnalysisHandler<*>,
    ) : WrappedException(cause, 1, 3) {
        override val failureDisablesNextSteps: Boolean
            get() = handler.failureDisablesNextSteps

        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromHandler(newCause, failedModule, handler)
        }
    }

    class FromGroupingHandler(
        cause: Throwable,
        val handler: GroupingStageHandler<*>,
    ) : WrappedException(cause, 1, 3) {
        override val failedModule: TestModule? get() = null

        override val failureDisablesNextSteps: Boolean
            get() = handler.failureDisablesNextSteps

        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromGroupingHandler(newCause, handler)
        }
    }

    sealed class WrappedExceptionWithoutModule(
        cause: Throwable,
        priority: Int,
        additionalPriority: Int,
    ) : WrappedException(cause, priority, additionalPriority) {
        override val failedModule: TestModule?
            get() = null
    }

    class FromPreAnalysisHandler(cause: Throwable) : WrappedExceptionWithoutModule(cause, 1, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromPreAnalysisHandler(newCause)
        }
    }

    class FromMetaInfoHandler(cause: Throwable) : WrappedExceptionWithoutModule(cause, 1, 2) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromMetaInfoHandler(newCause)
        }
    }

    class FromAfterAnalysisChecker(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromAfterAnalysisChecker(newCause)
        }
    }

    class FromFailingTestSuppressor(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 2) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromFailingTestSuppressor(newCause)
        }
    }

    class FromModuleStructureTransformer(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromModuleStructureTransformer(newCause)
        }
    }

    /**
     * `true` if this failure originates from the test infrastructure itself rather than from the code under test
     * (e.g. an internal invariant of the test runner was violated).
     *
     * Such failures must never be suppressed by [org.jetbrains.kotlin.test.model.TestFailureSuppressor]s
     * (for instance, by an `IGNORE_BACKEND` directive). Otherwise an infrastructure problem would be masked as a
     * green test, hiding the real (unknown) test status.
     */
    val isTestInfrastructureFailure: Boolean
        get() = cause is TestInfrastructureException

    final override val cause: Throwable
        get() = super.cause!!

    override fun compareTo(other: WrappedException): Int {
        if (priority == other.priority) {
            return additionalPriority - other.additionalPriority
        }
        return priority - other.priority
    }

    abstract fun withReplacedCause(newCause: Throwable): WrappedException
}
