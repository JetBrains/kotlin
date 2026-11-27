// KT-91575: the call site of the inlined lambda in the dead `tag` call must not add an SMAP range.
// FILE: 1.kt
package test

var log = ""

inline fun tag(block: () -> Unit) {
    log += "<"
    block()
    log += ">"
}

inline fun body(block: () -> Unit) = tag(block)

// FILE: 2.kt
import test.*

fun test() {
    body {
        tag { log += "a" }
        tag { throw IllegalStateException("boom") }
        tag { log += "dead" }
    }
}

fun box(): String {
    try {
        test()
    } catch (e: IllegalStateException) {
        return if (log == "<<a><") "OK" else "fail: $log"
    }
    return "fail: no exception"
}
