// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun <R> doCall(block: ()-> R, finallyBlock: ()-> Unit) : R {
    try {
        return block()
    } finally {
        finallyBlock()
    }
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}


fun test1(h: Holder) {
    val localResult = doCall (
            {
                h.value = "OK_NONLOCAL"
                return
            }, {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            })
}


fun test2(h: Holder) {
    val localResult = doCall (
            {
                h.value += "OK_LOCAL"
                "OK_LOCAL"
            }, {
                h.value += ", OK_FINALLY"
                return
            })
}

fun test3(h: Holder) {
    val localResult = doCall (
            {
                h.value += "OK_NONLOCAL"
                return
            }, {
                h.value += ", OK_FINALLY"
                return
            })
}


fun box(): String {
    var h = Holder()
    test1(h)
    if (h.value != "OK_NONLOCAL, OK_FINALLY") return "test1 holder: ${h.value}"

    h = Holder()
    test2(h)
    if (h.value != "OK_LOCAL, OK_FINALLY") return "test2 holder: ${h.value}"

    h = Holder()
    test3(h)
    if (h.value != "OK_NONLOCAL, OK_FINALLY") return "test3 holder: ${h.value}"

    return "OK"
}
