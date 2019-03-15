// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun <R> doCall(block: ()-> R) : R {
    return block()
}

// FILE: 2.kt

import test.*

fun test1(local: Int, nonLocal: String, doNonLocal: Boolean): String {

    val localResult = doCall(
            fun (): Int {
                if (doNonLocal) {
                    return@test1 nonLocal
                }
                return local
            })

    if (localResult == 11) {
        return "OK_LOCAL"
    }
    else {
        return "LOCAL_FAILED"
    }
}

fun test2(local: Int, nonLocal: String, doNonLocal: Boolean): String {

    val localResult = doCall(
            xxx@ fun(): Int {
                if (doNonLocal) {
                    return@test2 nonLocal
                }
                return@xxx local
            })

    if (localResult == 11) {
        return "OK_LOCAL"
    }
    else {
        return "LOCAL_FAILED"
    }
}

fun test3(local: Int, nonLocal: String, doNonLocal: Boolean): String {

    val localResult = doCall(
            yy@ fun(): Int {
                if (doNonLocal) {
                    return@test3 nonLocal
                }
                return@yy local
            })

    if (localResult == 11) {
        return "OK_LOCAL"
    }
    else {
        return "LOCAL_FAILED"
    }
}

fun box(): String {
    var test1 = test1(11, "fail", false)
    if (test1 != "OK_LOCAL") return "test1: ${test1}"

    test1 = test1(-1, "OK_NONLOCAL", true)
    if (test1 != "OK_NONLOCAL") return "test2: ${test1}"

    var test2 = test2(11, "fail", false)
    if (test2 != "OK_LOCAL") return "test1: ${test2}"

    test2 = test2(-1, "OK_NONLOCAL", true)
    if (test2 != "OK_NONLOCAL") return "test2: ${test2}"

    var test3 = test3(11, "fail", false)
    if (test3 != "OK_LOCAL") return "test1: ${test3}"

    test3 = test3(-1, "OK_NONLOCAL", true)
    if (test3 != "OK_NONLOCAL") return "test2: ${test3}"

    return "OK"
}
