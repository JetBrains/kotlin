// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

class Context {
    fun foo() = 1
}

context(Context)
class Test {
    fun foo() = 2
    fun bar() {
        val x = this@Context.foo()
    }
}
