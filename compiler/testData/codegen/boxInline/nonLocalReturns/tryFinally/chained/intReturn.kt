// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public class Holder {
    public var value: String = ""
}

public inline fun <R> doCall2_2(block: () -> R, res: R, h: Holder): R {
    return doCall2_1(block, {
        h.value += ", OK_EXCEPTION"
        "OK_EXCEPTION"
    }, res, h)
}

public inline fun <R> doCall2_1(block: () -> R, exception: (e: Exception) -> Unit, res: R, h: Holder): R {
    return doCall2<R>(block, exception, {
        h.value += ", OK_FINALLY"
        "OK_FINALLY"
    }, res)
}

public inline fun <R> doCall2(block: () -> R, exception: (e: Exception) -> Unit, finallyBlock: () -> Unit, res: R): R {
    try {
        return block()
    }
    catch (e: Exception) {
        exception(e)
    }
    finally {
        finallyBlock()
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
                    throw RuntimeException()
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
    if (test0 != -1 || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY") return "test0: ${test0}, holder: ${h.value}"

    h = Holder()
    val test1 = test0(h, false)
    if (test1 != 1 || h.value != "OK_NONLOCAL, OK_FINALLY") return "test1: ${test1}, holder: ${h.value}"

    return "OK"
}
