package m2

import m1.*

fun bar(): Int {
    foo { return 100 }
    return -99
}

fun box(): String {
    if (bar() == 100) return "OK"
    return "fail"
}