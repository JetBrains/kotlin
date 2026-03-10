package foo

import library.sample.*

var ok = "FAIL"

fun main() {
    val x = ClassA().value
    if (x == 100) {
        ok = "OK"
    }
}

fun box(): String = ok
