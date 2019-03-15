package foo

var ok = "FAIL"

fun main(args: kotlin.Array<kotlin.String>) {
    args.size
    ok = "OK"
}

fun box(): String = ok
