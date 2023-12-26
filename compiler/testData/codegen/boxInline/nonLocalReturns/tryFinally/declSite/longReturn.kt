// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public inline fun doCall(block: ()-> Long, exception: (e: Exception)-> Unit, finallyBlock: ()-> Long, res: Long = -1111.toLong()) : Long {
    try {
        block()
    } catch (e: Exception) {
        exception(e)
    } finally {
        finallyBlock()
    }
    return res
}

public inline fun <R> doCall2(block: ()-> R, exception: (e: Exception)-> Unit, finallyBlock: ()-> R, res: R) : R {
    try {
        return block()
    } catch (e: Exception) {
        exception(e)
    } finally {
        finallyBlock()
    }
    return res
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}

fun test0(h: Holder): Long {
    val localResult = doCall2 (
            {
                h.value += "OK_NONLOCAL"
                if (true) {
                    throw RuntimeException()
                }
                return 1.toLong()
            },
            {
                h.value += ", OK_EXCEPTION"
                return 2.toLong()
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            }, "FAIL")

    return -1.toLong()
}

fun test1(h: Holder): Long {
    val localResult = doCall2 (
            {
                h.value += "OK_NONLOCAL"
                return 1.toLong()
            },
            {
                h.value += ", OK_EXCEPTION"
                "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            }, "FAIL")

    return -1.toLong()
}

fun test2(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NONLOCAL"
                return "OK_NONLOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                2.toLong()
            },
            {
                h.value += ", OK_FINALLY"
                3.toLong()
            })

    return "" + localResult;
}

fun test3(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NONLOCAL"
                if (true) {
                    throw RuntimeException()
                }
                return "OK_NONLOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                return "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                3.toLong()
            })

    return "" + localResult;
}

fun test4(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NONLOCAL"
                if (true) {
                    throw RuntimeException()
                }
                h.value += "fail"
                return "OK_NONLOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                return "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                return "OK_FINALLY"
            })

    return "" + localResult;
}

fun box(): String {
    var h = Holder()
    val test0 = test0(h)
    if (test0 != 2.toLong() || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test0: ${test0}, holder: ${h.value}"

    h = Holder()
    val test1 = test1(h)
    if (test1 != 1.toLong() || h.value != "OK_NONLOCAL, OK_FINALLY") return "test1: ${test1}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h)
    if (test2 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OK_FINALLY") return "test2: ${test2}, holder: ${h.value}"

    h = Holder()
    val test3 = test3(h)
    if (test3 != "OK_EXCEPTION" || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test3: ${test3}, holder: ${h.value}"

    h = Holder()
    val test4 = test4(h)
    if (test4 != "OK_FINALLY" || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test4: ${test4}, holder: ${h.value}"

    return "OK"
}
