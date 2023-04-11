// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

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
