// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

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
