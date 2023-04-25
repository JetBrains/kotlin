// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

class Outer {
    val x: Int = 1
}

context(Outer)
class Inner(arg: Any) {
    fun bar() = x
}

fun f(outer: Outer) {
    with(outer) {
        Inner(3)
    }
}
