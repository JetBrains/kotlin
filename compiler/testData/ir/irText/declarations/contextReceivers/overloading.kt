// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// DUMP_LOCAL_DECLARATION_SIGNATURES
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57428, KT-57435

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
