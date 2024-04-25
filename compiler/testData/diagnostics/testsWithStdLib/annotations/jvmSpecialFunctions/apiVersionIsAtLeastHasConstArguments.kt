// ALLOW_KOTLIN_PACKAGE
// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// FILE: test.kt
package test

import kotlin.internal.*

const val ZERO = 0
const val ONE = 1

fun zero() = 0
val one = 1

val test0 = apiVersionIsAtLeast(0, 0, 0)
val testConstVals = apiVersionIsAtLeast(ONE, ONE, ZERO)
val testConstExprs = apiVersionIsAtLeast(ONE + 0, 1 + 0, ((0 + 1 + 0)))
val testNonConstExprs = apiVersionIsAtLeast(<!API_VERSION_IS_AT_LEAST_ARGUMENT_SHOULD_BE_CONSTANT!>one<!>, <!API_VERSION_IS_AT_LEAST_ARGUMENT_SHOULD_BE_CONSTANT!>zero()<!>, <!API_VERSION_IS_AT_LEAST_ARGUMENT_SHOULD_BE_CONSTANT!>one + 1<!>)

// FILE: apiVersionIsAtLeast.kt
package kotlin.internal

fun apiVersionIsAtLeast(epic: Int, major: Int, minor: Int): Boolean =
        false