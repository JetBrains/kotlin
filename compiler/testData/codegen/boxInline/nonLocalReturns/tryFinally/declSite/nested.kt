// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun doCall(block: ()-> Unit, finallyBlock1: ()-> Unit, finallyBlock2: ()-> Unit) {
    try {
        try {
            block()
        }
        finally {
            finallyBlock1()
        }
    } finally {
        finallyBlock2()
    }
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}


fun test1(h: Holder): String {
    doCall (
            {
                h.value += "OK_LOCAL"
            },
            {
                h.value += ", OK_FINALLY1"
            },
            {
                h.value += ", OK_FINALLY2"
            }
    )

    return "LOCAL";
}


fun test2(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NONLOCAL"
                return "OK_NONLOCAL"
            },
            {
                h.value += ", OK_FINALLY1"
            },
            {
                h.value += ", OK_FINALLY2"
            })

    return "FAIL";
}

fun test3(h: Holder): String {
    doCall (
            {
                h.value += "OK_LOCAL"
            },
            {
                h.value += ", OK_FINALLY1"
                return "OK_FINALLY1"
            },
            {
                h.value += ", OK_FINALLY2"
            })

    return "FAIL";
}

fun test4(h: Holder): String {
    doCall (
            {
                h.value += "OK_LOCAL"
            },
            {
                h.value += ", OK_FINALLY1"
            },
            {
                h.value += ", OK_FINALLY2"
                return "OK_FINALLY2"
            })

    return "FAIL";
}

fun test5(h: Holder): String {
    doCall (
            {
                h.value += "OK_LOCAL"
            },
            {
                h.value += ", OK_FINALLY1"
                  return "OK_FINALLY1"
            },
            {
                h.value += ", OK_FINALLY2"
                return "OK_FINALLY2"
            })

    return "FAIL";
}

fun box(): String {
    var h = Holder()
    val test1 = test1(h)
    if (test1 != "LOCAL" || h.value != "OK_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test1: ${test1}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h)
    if (test2 != "OK_NONLOCAL" || h.value != "OK_NONLOCAL, OK_FINALLY1, OK_FINALLY2") return "test2: ${test2}, holder: ${h.value}"

    h = Holder()
    val test3 = test3(h)
    if (test3 != "OK_FINALLY1" || h.value != "OK_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test3: ${test3}, holder: ${h.value}"

    h = Holder()
    val test4 = test4(h)
    if (test4 != "OK_FINALLY2" || h.value != "OK_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test4: ${test4}, holder: ${h.value}"

    h = Holder()
    val test5 = test5(h)
    if (test5 != "OK_FINALLY2" || h.value != "OK_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test5: ${test5}, holder: ${h.value}"


    return "OK"
}
