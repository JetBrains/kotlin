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
