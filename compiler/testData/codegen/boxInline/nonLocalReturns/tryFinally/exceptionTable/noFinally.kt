// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public inline fun  doCall(block: ()-> String, exception: (e: Exception)-> Unit) : String {
    try {
        return block()
    } catch (e: Exception) {
        exception(e)
    }
    return "Fail in doCall"
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
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
            })

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
            })

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
            })

    return localResult;
}

fun test5(h: Holder): String {
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
                        throw RuntimeException("EXCEPTION")
                    }
                    h.value += "fail"

                    return "OK_EXCEPTION"
                })

        return localResult;
    } catch (e: RuntimeException) {
        if (e.message != "EXCEPTION") {
            return "FAIL in exception: " + e.message
        } else {
            return "CATCHED_EXCEPTION"
        }
    }
}

fun box(): String {
    var h = Holder()
    val test2 = test2(h)
    if (test2 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL") return "test2: ${test2}, holder: ${h.value}"

    h = Holder()
    val test3 = test3(h)
    if (test3 != "OK_EXCEPTION" || h.value != "OK_NONLOCAL, OK_EXCEPTION") return "test3: ${test3}, holder: ${h.value}"

    h = Holder()
    val test4 = test4(h)
    if (test4 != "OK_EXCEPTION" || h.value != "OK_NONLOCAL, OK_EXCEPTION") return "test4: ${test4}, holder: ${h.value}"

    h = Holder()
    val test5 = test5(h)
    if (test5 != "CATCHED_EXCEPTION" || h.value != "OK_NONLOCAL, OK_EXCEPTION") return "test5: ${test5}, holder: ${h.value}"

    return "OK"
}
