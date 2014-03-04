package foo

var ok = "FAIL"

fun main(vararg args: jet.String) {
    ok = "OK"
}

fun box(): String = ok
