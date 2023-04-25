// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

class Context

context(Context)
fun f(): String = TODO()

fun f(): Any = TODO()

fun test() {
    with(Context()) {
        f().length
    }
}
