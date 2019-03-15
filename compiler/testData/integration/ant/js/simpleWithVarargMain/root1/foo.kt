package foo

var ok = "FAIL"

fun main(vararg args: kotlin.String) {
    args.size
    ok = "OK"
}

fun box(): String = ok
