package foo

var ok = "FAIL"

fun main() {
    ok = "OK"
}

fun box(): String = ok
