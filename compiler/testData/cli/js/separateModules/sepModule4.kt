package m4

import m3.*

inline fun foo(action: () -> Int): Int {
    return action()
}

fun bar(): Int {
    foo { return 100 }
    return -99
}

fun box(): String {
    if (bar() == 100) return "OK"
    return "fail"
}