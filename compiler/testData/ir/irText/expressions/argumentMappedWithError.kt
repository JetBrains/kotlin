// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun <R : Number> Number.convert(): R = TODO()

fun foo(arg: Number) {
}

fun main(args: Array<String>) {
    val x: Int = 0
    foo(x.convert())
}
