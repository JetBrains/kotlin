package foo

var ok = "FAIL"

val hello = Pair("Hello", "World")

fun main(args: Array<String>) {
    ok = "OK"
    println(hello.first + " " + hello.second)
}

fun box(): String = ok
