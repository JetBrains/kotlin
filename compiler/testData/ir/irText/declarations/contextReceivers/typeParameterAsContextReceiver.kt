// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

context(T)
fun <T> useContext(block: (T) -> Unit) { }

fun test() {
    with(42) {
        useContext { i: Int -> i.toDouble() }
    }
}
