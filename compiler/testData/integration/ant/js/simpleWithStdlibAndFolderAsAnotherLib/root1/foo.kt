package foo

import library.sample.*

var ok = "FAIL"

fun main(args: Array<String>) {
    val p = Pair(10, 20)
    val x = pairAdd(p)
    ok = "OK"
    println("x=$x")
}

fun box(): String = ok
