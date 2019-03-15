// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class Holder {
    var value: String = ""
}

inline fun doCall_1(block: ()-> Unit, h: Holder) {
    try {
        doCall(block) {
            h.value += ", OF_FINALLY1"
        }
    } finally {
        h.value += ", DO_CALL_1_FINALLY"
    }
}

inline fun doCall_2(block: ()-> Unit, h: Holder) {
    try {
        doCall(block) {
            try {
                h.value += ", OF_FINALLY1"
            }
            finally {
                h.value += ", OF_FINALLY1_FINALLY"
            }
        }
    } finally {
        h.value += ", DO_CALL_1_FINALLY"
    }
}

inline fun doCall(block: ()-> Unit, finallyBlock1: ()-> Unit) {
    try {
         block()
    } finally {
        finallyBlock1()
    }
}

// FILE: 2.kt

import test.*

fun test1(h: Holder, doReturn: Int): String {
    doCall_1 (
            {
                if (doReturn < 1) {
                    h.value += "OK_NONLOCAL"
                    return "OK_NONLOCAL"
                }
                h.value += "LOCAL"
                "OK_LOCAL"
            },
            h
    )

    return "TEST1";
}

fun test2(h: Holder, doReturn: Int): String {
    doCall_2 (
            {
                if (doReturn < 1) {
                    h.value += "OK_NONLOCAL"
                    return "OK_NONLOCAL"
                }
                h.value += "LOCAL"
                "OK_LOCAL"
            },
            h
    )

    return "TEST2";
}

fun box(): String {
    var h = Holder()
    val test10 = test1(h, 0)
    if (test10 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OF_FINALLY1, DO_CALL_1_FINALLY") return "test10: ${test10}, holder: ${h.value}"

    h = Holder()
    val test11 = test1(h, 1)
    if (test11 != "TEST1" || h.value != "LOCAL, OF_FINALLY1, DO_CALL_1_FINALLY") return "test11: ${test11}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h, 0)
    if (test2 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OF_FINALLY1, OF_FINALLY1_FINALLY, DO_CALL_1_FINALLY") return "test20: ${test2}, holder: ${h.value}"

    h = Holder()
    val test21 = test2(h, 1)
    if (test21 != "TEST2" || h.value != "LOCAL, OF_FINALLY1, OF_FINALLY1_FINALLY, DO_CALL_1_FINALLY") return "test21: ${test21}, holder: ${h.value}"

    return "OK"
}
