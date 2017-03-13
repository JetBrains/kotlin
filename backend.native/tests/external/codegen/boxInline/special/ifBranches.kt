// FILE: 1.kt

package test

inline fun <T> runIf(f: (T) -> T, start: T, stop: T, secondStart: T) : T {
    if (f(start) == stop) {
        return f(start)
    }
    return f(secondStart)
}


inline fun <T> runIf2(f: (T) -> T, start: T, stop: T, secondStart: T) : T {
    val result = f(start)
    if (result == stop) {
        return result
    }
    return f(secondStart)
}

inline fun <T> runIfElse(f: (T) -> T, start: T, stop: T, secondStart: T) : T {
    if (f(start) == stop) {
        return f(start)
    } else {
        return f(secondStart)
    }
}

// FILE: 2.kt

import test.*

fun testIf(): String {
    if (runIf({it}, 11, 11, 12) != 11) return "testIf 1 test fail"
    if (runIf({it}, 11, 1, 12) != 12) return "testIf 2 test fail"

    if (runIf({if (it == 11) it else 12}, 11, 11, 0) != 11) return "testIf 3 test fail"
    if (runIf({if (it == 11) it else 12}, 11, 1, 0) != 12) return "testIf 4 test fail"

    return "OK"
}

fun testIf2(): String {
    if (runIf2({it}, 11, 11, 12) != 11) return "testIf2 1 test fail"
    if (runIf2({it}, 11, 1, 12) != 12) return "testIf2 2 test fail"

    if (runIf2({if (it == 11) it else 12}, 11, 11, 0) != 11) return "testIf2 3 test fail"
    if (runIf2({if (it == 11) it else 12}, 11, 1, 0) != 12) return "testIf2 4 test fail"

    return "OK"
}

fun testIfElse(): String {
    if (runIfElse({it}, 11, 11, 12) != 11) return "testIfElse 1 test fail"
    if (runIfElse({it}, 11, 1, 12) != 12) return "testIfElse 2 test fail"

    if (runIfElse({if (it == 11) it else 12}, 11, 11, 0) != 11) return "testIfElse 3 test fail"
    if (runIfElse({if (it == 11) it else 12}, 11, 1, 0) != 12) return "testIfElse 4 test fail"
    return "OK"
}

fun box(): String {
    var result = testIf()
    if (result != "OK") return "fail1: ${result}"

    result = testIf2()
    if (result != "OK") return "fail2: ${result}"

    result = testIfElse()
    if (result != "OK") return "fail2: ${result}"

    return "OK"
}
