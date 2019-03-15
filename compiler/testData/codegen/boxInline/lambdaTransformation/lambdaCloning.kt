// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun <T> doSmth(a: T) : String {
    return {a.toString()}()
}

inline fun <T> doSmth2(a: T) : String {
    return {{a.toString()}()}()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun test1(s: Long): String {
    return doSmth(s)
}

fun test2(s: Int): String {
    return doSmth2(s)
}

fun box(): String {
    var result = test1(11.toLong())
    if (result != "11") return "fail1: ${result}"

    result = test2(11)
    if (result != "11") return "fail2: ${result}"

    return "OK"
}
