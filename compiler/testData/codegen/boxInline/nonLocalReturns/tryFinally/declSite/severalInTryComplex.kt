// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class Holder {
    var value: String = ""
}

inline fun doCall(block: ()-> Unit, block2: ()-> Unit, finallyBlock2: ()-> Unit, res: Holder) {
    try {
        try {
            block()
            block2()
        }
        finally {
            finallyBlock2()
        }
    } finally {
        res.value += ", DO_CALL_EXT_FINALLY"
    }
}

// FILE: 2.kt

import test.*

fun test1(h: Holder, doReturn: Int): String {
    doCall (
            {
                if (doReturn < 1) {
                    h.value += "OK_NONLOCAL"
                    return "OK_NONLOCAL"
                }
                h.value += "LOCAL"
                "OK_LOCAL"
            },
            {
                h.value += ", OK_NONLOCAL2"
                return "OK_NONLOCAL2"
            },
            {
                h.value += ", OK_FINALLY"
            },
            h
    )

    return "LOCAL";
}


fun test2(h: Holder, doReturn: Int): String {
    doCall (
            {
                if (doReturn < 1) {
                    h.value += "OK_NONLOCAL"
                    return "OK_NONLOCAL"
                }
                h.value += "LOCAL"
                "OK_LOCAL"
            },
            {
                try {
                    h.value += ", OK_NONLOCAL2"
                    return "OK_NONLOCAL2"
                } finally {
                    h.value += ", OK_NONLOCAL2_FINALLY"
                }
            },
            {
                h.value += ", OK_FINALLY"
            },
            h
    )

    return "FAIL";
}

fun box(): String {
    var h = Holder()
    val test10 = test1(h, 0)
    if (test10 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OK_FINALLY, DO_CALL_EXT_FINALLY") return "test10: ${test10}, holder: ${h.value}"

    h = Holder()
    val test11 = test1(h, 1)
    if (test11 != "OK_NONLOCAL2" || h.value != "LOCAL, OK_NONLOCAL2, OK_FINALLY, DO_CALL_EXT_FINALLY") return "test11: ${test11}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h, 0)
    if (test2 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OK_FINALLY, DO_CALL_EXT_FINALLY") return "test20: ${test2}, holder: ${h.value}"

    h = Holder()
    val test21 = test2(h, 1)
    if (test21 != "OK_NONLOCAL2" || h.value != "LOCAL, OK_NONLOCAL2, OK_NONLOCAL2_FINALLY, OK_FINALLY, DO_CALL_EXT_FINALLY") return "test21: ${test21}, holder: ${h.value}"

    return "OK"
}
