// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public inline fun <R> doCall(block: ()-> R, exception: (e: Exception)-> Unit, finallyBlock: ()-> R, res: R) : R {
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

fun test0(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_LOCAL"
                "OK_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            }, "Fail")

    return localResult;
}

fun test1(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_LOCAL"
                throw RuntimeException()
                "OK_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            }, "OK")

    return localResult;
}

fun test2(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NONLOCAL"
                return "OK_NONLOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            }, "FAIL")

    return localResult;
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
                "OK_FINALLY"
            }, "FAIL")

    return localResult;
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
            }, "FAIL")

    return localResult;
}

fun test5(h: Holder): String {
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
                if (true) {
                    throw RuntimeException()
                }
                h.value += "fail"

                return "OK_EXCEPTION"
            },
            {
                h.value += ", OK_FINALLY"
                return "OK_FINALLY"
            }, "FAIL")

    return localResult;
}


fun test6(h: Holder): String {
    try {
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
                    if (true) {
                        throw RuntimeException()
                    }
                    h.value += "fail"

                    return "OK_EXCEPTION"
                },
                {
                    h.value += ", OK_FINALLY"
                    "OK_FINALLY"
                },
                "FAIL1")
    } catch (e: Exception) {
        return "OK"
    }

    return "FAIL2";
}

fun box(): String {
    var h = Holder()
    val test0 = test0(h)
    if (test0 != "OK_LOCAL" || h.value != "OK_LOCAL, OK_FINALLY") return "test0: ${test0}, holder: ${h.value}"


    h = Holder()
    val test1 = test1(h)
    if (test1 != "OK" || h.value != "OK_LOCAL, OK_EXCEPTION, OK_FINALLY") return "test1: ${test1}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h)
    if (test2 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OK_FINALLY") return "test2: ${test2}, holder: ${h.value}"

    h = Holder()
    val test3 = test3(h)
    if (test3 != "OK_EXCEPTION" || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test3: ${test3}, holder: ${h.value}"

    h = Holder()
    val test4 = test4(h)
    if (test4 != "OK_FINALLY" || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test4: ${test4}, holder: ${h.value}"

    h = Holder()
    val test5 = test5(h)
    if (test5 != "OK_FINALLY" || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test5: ${test5}, holder: ${h.value}"

    h = Holder()
    val test6 = test6(h)
    if (test6 != "OK" || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test6: ${test6}, holder: ${h.value}"

    return "OK"
}
