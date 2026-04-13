class Context {
    fun foo() {

    }
}

context(param: Context)
val check: Unit
    get() {
       param.<expr>foo()</expr>
    }

// LANGUAGE: +ContextParameters
