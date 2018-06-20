// IGNORE_BACKEND: JS_IR
// MODULE: lib
// FILE: lib.kt

package utils

inline fun foo(a: Int) {
    try {
        if (a > 0) throw Exception()
        log("foo($a)")
    }
    catch (e: Exception) {
        bar(a)
    }
}

inline fun bar(a: Int) {
    myRun {
        log("bar($a) #1")
        if (a == 2) return
        log("bar($a) #2")
    }
}

var LOG: String = ""

fun log(s: String): String {
    LOG += s + ";"
    return LOG
}

inline fun myRun(f: () -> Unit) = f()

// MODULE: main(lib)
// FILE: main.kt

import utils.*

fun box(): String {
    foo(0)
    if (LOG != "foo(0);") return "fail1: $LOG"
    LOG = ""

    foo(1)
    if (LOG != "bar(1) #1;bar(1) #2;") return "fail2: $LOG"
    LOG = ""

    foo(2)
    if (LOG != "bar(2) #1;") return "fail3: $LOG"

    return "OK"
}