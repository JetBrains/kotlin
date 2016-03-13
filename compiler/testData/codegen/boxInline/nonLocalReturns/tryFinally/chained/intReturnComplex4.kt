// FILE: 1.kt

package test

public class Holder {
    public var value: String = ""
}

public inline fun doCall2_2(block: () -> String, res: String, h: Holder): String {
    try {
        return doCall2_1(block, {
            h.value += ", OK_EXCEPTION"
            return "OK_EXCEPTION"
        }, res, h)
    } finally{
        h.value += ", DO_CALL_2_2_FINALLY"
    }
}

public inline fun <R> doCall2_1(block: () -> R, exception: (e: Exception) -> Unit, res: R, h: Holder): R {
    try {
        return doCall2<R>(block, exception, {
            try {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            } finally {
                h.value += ", OK_FINALLY_NESTED"
            }
        }, res, h)
    } finally {
        h.value += ", DO_CALL_2_1_FINALLY"
    }
}

public inline fun <R> doCall2(block: () -> R, exception: (e: Exception) -> Unit, finallyBlock: () -> Unit, res: R, h: Holder): R {
    try {
        try {
            return block()
        }
        catch (e: Exception) {
            exception(e)
        }
        finally {
            finallyBlock()
        }
    } finally {
        h.value += ", DO_CALL_2_FINALLY"
    }
    return res
}

// FILE: 2.kt

import test.*


fun test0(h: Holder, throwException: Boolean): Int {
    val localResult = doCall2_2 (
            {
                h.value += "OK_NONLOCAL"
                if (throwException) {
                    throw java.lang.RuntimeException()
                }
                return 1
            },
            "FAIL",
            h
    )

    return -1;
}


fun box(): String {
    var h = Holder()
    val test0 = test0(h, true)

    if (test0 != -1 || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY, OK_FINALLY_NESTED, DO_CALL_2_FINALLY, DO_CALL_2_1_FINALLY, DO_CALL_2_2_FINALLY") return "test0: ${test0}, holder: ${h.value}"

    h = Holder()
    val test1 = test0(h, false)
    if (test1 != 1 || h.value != "OK_NONLOCAL, OK_FINALLY, OK_FINALLY_NESTED, DO_CALL_2_FINALLY, DO_CALL_2_1_FINALLY, DO_CALL_2_2_FINALLY") return "test1: ${test1}, holder: ${h.value}"

    return "OK"
}
