// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_FIR: JVM_IR

class Context

context(Context)
fun f(): String = TODO()

fun f(): Any = TODO()

fun test() {
    with(Context()) {
        f().length
    }
}