// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

context(Int, String)
fun foo(): Int {
    return this@Int + 42
}

context(Int)
fun foo(): Int {
    return this@Int + 42
}

fun test() {
    with(42) {
        foo()
    }
}
