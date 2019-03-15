// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun doCall(block: ()-> Unit, finallyBlock1: ()-> Unit) {
    try {
         block()
    } finally {
        finallyBlock1()
    }
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}


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
                h.value += ", OF_FINALLY1"
                return "OF_FINALLY1"
            }
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
                    h.value += ", OF_FINALLY1"
                    return "OF_FINALLY1"
                } finally {
                    h.value += ", OF_FINALLY1_FINALLY"
                }
            }
    )

    return "FAIL";
}

fun box(): String {
    var h = Holder()
    val test10 = test1(h, 0)
    if (test10 != "OF_FINALLY1" || h.value != "OK_NONLOCAL, OF_FINALLY1") return "test10: ${test10}, holder: ${h.value}"

    h = Holder()
    val test11 = test1(h, 1)
    if (test11 != "OF_FINALLY1" || h.value != "LOCAL, OF_FINALLY1") return "test11: ${test11}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h, 0)
    if (test2 != "OF_FINALLY1" || h.value != "OK_NONLOCAL, OF_FINALLY1, OF_FINALLY1_FINALLY") return "test20: ${test2}, holder: ${h.value}"

    h = Holder()
    val test21 = test2(h, 1)
    if (test21 != "OF_FINALLY1" || h.value != "LOCAL, OF_FINALLY1, OF_FINALLY1_FINALLY") return "test21: ${test21}, holder: ${h.value}"

    return "OK"
}
