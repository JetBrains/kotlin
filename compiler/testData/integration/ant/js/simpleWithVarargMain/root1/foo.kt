package foo

var ok = "FAIL"

fun main(vararg args: kotlin.String) {
    ok = "OK"
}

fun box(): String = ok
