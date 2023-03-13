// FIR_IDENTICAL
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun <R : Number> Number.convert(): R = TODO()

fun foo(arg: Number) {
}

fun main(args: Array<String>) {
    val x: Int = 0
    foo(x.convert())
}
