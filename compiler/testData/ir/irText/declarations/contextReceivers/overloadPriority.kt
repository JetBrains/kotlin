// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// DUMP_LOCAL_DECLARATION_SIGNATURES
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57428

class Context

context(Context)
fun f(): String = TODO()

fun f(): Any = TODO()

fun test() {
    with(Context()) {
        f().length
    }
}
