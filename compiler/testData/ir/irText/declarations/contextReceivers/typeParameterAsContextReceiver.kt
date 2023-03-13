// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

context(T)
fun <T> useContext(block: (T) -> Unit) { }

fun test() {
    with(42) {
        useContext { i: Int -> i.toDouble() }
    }
}
