/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import kotlin.contracts.*

/**
 * Signals a failure caused by the test infrastructure itself (a violated internal invariant of the test runner,
 * handlers, etc.) as opposed to a failure of the code under test (e.g. a compiler bug).
 *
 * Failures wrapped around a [TestInfrastructureException] are intentionally **not** suppressed by
 * [org.jetbrains.kotlin.test.model.TestFailureSuppressor]s, even when the test carries a matching mute directive such
 * as `IGNORE_BACKEND`. This guarantees that an infrastructure problem is always reported instead of being silently
 * turned into a green test, which would mask the real (unknown) test status.
 *
 * Use it instead of [check]/[error] for asserting test-infrastructure invariants when the resulting failure could be
 * processed by failure suppressors.
 */
class TestInfrastructureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Throws a [TestInfrastructureException] if [value] is `false`.
 *
 * The drop-in replacement for [check] for test-infrastructure invariants whose violation must never be masked by
 * failure suppressors (see [TestInfrastructureException]).
 */
@OptIn(ExperimentalContracts::class)
inline fun checkTestInfrastructure(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }
    if (!value) {
        testInfraError(lazyMessage())
    }
}

fun testInfraError(message: String): Nothing = throw TestInfrastructureException(message)
