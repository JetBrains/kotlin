/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.AnalysisHandler
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

    class FromModuleStructureTransformer(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromModuleStructureTransformer(newCause)
        }
    }

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
