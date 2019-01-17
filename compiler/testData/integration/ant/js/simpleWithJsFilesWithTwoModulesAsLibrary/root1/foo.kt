package foo

import library.sample.*
import kotlin.js.Date

var ok = "FAIL"

fun main() {
    val x = ClassA().value
    if (x == 100 && Date().extFun() == "Date.extFun" && ClassA().extFun() == "ClassA.extFun") {
        ok = "OK"
    }
}

fun box(): String = ok
