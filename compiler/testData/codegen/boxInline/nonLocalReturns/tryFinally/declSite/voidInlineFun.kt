// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun <R> doCall(block: ()-> R, finallyBlock: ()-> Unit) {
    try {
        block()
    } finally {
        finallyBlock()
    }
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}


fun test1(h: Holder): String {
    doCall ({
        return "OK_NONLOCAL"
    }, {
        h.value = "OK_FINALLY"
    })

    return "FAIL";
}


fun test2(h: Holder): String {
    doCall ({
      h.value += "OK_LOCAL"
      "OK_LOCAL"
    }, {
      h.value += ", OK_FINALLY"
      return "OK_FINALLY"
    })

    return "FAIL";
}

fun test3(h: Holder): String {
    doCall ({
      h.value += "OK_NONLOCAL"
      return "OK_NONLOCAL"
    }, {
      h.value += ", OK_FINALLY"
      return "OK_FINALLY"
    })

    return "FAIL";
}


fun box(): String {
    var h = Holder()
    val test1 = test1(h)
    if (test1 != "OK_NONLOCAL" || h.value != "OK_FINALLY") return "test1: ${test1}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h)
    if (test2 != "OK_FINALLY" || h.value != "OK_LOCAL, OK_FINALLY") return "test2: ${test2}, holder: ${h.value}"

    h = Holder()
    val test3 = test3(h)
    if (test3 != "OK_FINALLY" || h.value != "OK_NONLOCAL, OK_FINALLY") return "test3: ${test3}, holder: ${h.value}"

    return "OK"
}
