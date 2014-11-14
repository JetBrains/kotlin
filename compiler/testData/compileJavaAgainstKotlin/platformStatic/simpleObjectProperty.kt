package test

import kotlin.platform.platformStatic

object A {
    [platformStatic] val b: String = "OK"

    var A.c: String
        [platformStatic]get() = "OK"
        [platformStatic]set(t: String) {}

}

fun main(args: Array<String>) {
    A.b
    with(A) {
        A.c
        A.c = "123"
    }
}