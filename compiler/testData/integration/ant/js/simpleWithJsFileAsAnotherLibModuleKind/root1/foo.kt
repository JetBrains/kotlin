package foo

import library.sample.*
import kotlin.js.Date

var ok = "FAIL"

fun main() {
    val x = ClassA().value
    if (x == 100) {
        ok = "OK"
    }
    val date = Date()
    println(date.extFun())
}

fun box(): String = ok
