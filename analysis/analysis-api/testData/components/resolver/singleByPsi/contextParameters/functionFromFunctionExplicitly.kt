class Context {
    fun foo() {

    }
}

context(param: Context)
fun check() {
   param.<expr>foo()</expr>
}

// LANGUAGE: +ContextParameters
