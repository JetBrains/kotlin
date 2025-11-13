// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 1.9.20 2.0.0 2.1.0 2.2.0
// ^^^ function `test` is codegenerated to `test_0`, and test directive `// CHECK_CONTAINS_NO_CALLS: test` does not see it

// MODULE: lib
// FILE: lib.kt

package utils

inline public fun <T> composition(x0: T, x1: T, x2: T, fn: (T, T) -> T): T = fn(fn(x0, x1), x2)

// MODULE: main(lib)
// FILE: main.kt

import utils.*

public fun nonInlinableConcat(x: String, y: String): String = "$x$y"

inline fun appendTo(target: String, suffix: String): String = nonInlinableConcat(target, suffix)

// CHECK_CONTAINS_NO_CALLS: test except=nonInlinableConcat
internal fun test(x: String): String = composition("", "O", "K", ::appendTo)

fun box(): String {
    return test("O")
}
