package foo

var ok = "FAIL"

fun main(args: jet.Array<jet.String>) {
    ok = "OK"
}

fun box(): String = ok
