package foo

var ok = "FAIL"

fun main(args: kotlin.Array<kotlin.String>) {
    ok = "OK"
}

fun box(): String = ok
