/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

sealed class WrappedException(
    cause: Throwable,
    val priority: Int,
    val additionalPriority: Int
) : Exception(cause), Comparable<WrappedException> {
    sealed class FromFacade(cause: Throwable, additionalPriority: Int) : WrappedException(cause, 0, additionalPriority) {
        class Frontend(cause: Throwable) : FromFacade(cause, 1)
        class Converter(cause: Throwable) : FromFacade(cause, 2)
        class Backend(cause: Throwable) : FromFacade(cause, 3)

        override val message: String
            get() = "Exception was thrown"
    }

    class FromMetaInfoHandler(cause: Throwable) : WrappedException(cause, 1, 1)

    class FromFrontendHandler(cause: Throwable) : WrappedException(cause, 1, 1)
    class FromBackendHandler(cause: Throwable) : WrappedException(cause, 1, 2)
    class FromBinaryHandler(cause: Throwable) : WrappedException(cause, 1, 3)
    class FromUnknownHandler(cause: Throwable) : WrappedException(cause, 1, 4)

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
