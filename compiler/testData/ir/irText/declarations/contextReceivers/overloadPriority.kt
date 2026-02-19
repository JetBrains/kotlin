// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K2: ANY

class Context

context(Context)
fun f(): String = TODO()

fun f(): Any = TODO()

fun test() {
    with(Context()) {
        f().length
    }
}
