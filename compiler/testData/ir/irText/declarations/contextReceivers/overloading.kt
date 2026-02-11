// IGNORE_BACKEND_K2: ANY
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers, -ContextParameters

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
