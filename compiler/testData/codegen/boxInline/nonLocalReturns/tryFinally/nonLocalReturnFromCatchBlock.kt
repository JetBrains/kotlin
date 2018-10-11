// IGNORE_BACKEND: JVM_IR
// MODULE: lib
// FILE: lib.kt

package utils

inline fun foo(a: Int) {
    bar(a)
}

inline fun bar(a: Int) {
    try {
        if (a > 0) throw Exception()
        log("foo($a) #1")
    }
    catch (e: Exception) {
        myRun {
            log("foo($a) #2")
            if (a > 1) return
            log("foo($a) #3")
        }
    }
    log("foo($a) #4")
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
    if (LOG != "foo(0) #1;foo(0) #4;") return "fail1: $LOG"
    LOG = ""

    foo(1)
    if (LOG != "foo(1) #2;foo(1) #3;foo(1) #4;") return "fail2: $LOG"
    LOG = ""

    foo(2)
    if (LOG != "foo(2) #2;") return "fail3: $LOG"
    LOG = ""

    return "OK"
}