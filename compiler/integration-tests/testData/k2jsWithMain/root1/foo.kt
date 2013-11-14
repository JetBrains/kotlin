package foo

var ok = "FAIL"

fun main(args: Array<String>) {
    ok = "OK"
}

fun box(): String = ok
