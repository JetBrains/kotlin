// FIR_IDENTICAL
// IGNORE_BACKEND: JKLIB

fun <R : Number> Number.convert(): R = TODO()

fun foo(arg: Number) {
}

fun runMe(args: Array<String>) {
    val x: Int = 0
    foo(x.convert())
}
