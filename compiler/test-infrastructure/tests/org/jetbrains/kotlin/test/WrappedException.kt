/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.AnalysisHandler

sealed class WrappedException(
    cause: Throwable,
    val priority: Int,
    val additionalPriority: Int
) : Exception(cause), Comparable<WrappedException> {
    class FromFacade(cause: Throwable, val facade: AbstractTestFacade<*, *>) : WrappedException(cause, 0, 1) {
        override val message: String
            get() = "Exception was thrown"
    }

    class FromMetaInfoHandler(cause: Throwable) : WrappedException(cause, 1, 1)

    class FromHandler(cause: Throwable, val handler: AnalysisHandler<*>) : WrappedException(cause, 1, 2)

    class FromAfterAnalysisChecker(cause: Throwable) : WrappedException(cause, 2, 1)

    class FromModuleStructureTransformer(cause: Throwable) : WrappedException(cause, 2, 1)

    override val cause: Throwable
        get() = super.cause!!

    override fun compareTo(other: WrappedException): Int {
        if (priority == other.priority) {
            return additionalPriority - other.additionalPriority
        }
        return priority - other.priority
    }
}
