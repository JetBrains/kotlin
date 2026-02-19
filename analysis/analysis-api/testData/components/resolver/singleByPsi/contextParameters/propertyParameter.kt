class Context {
    fun foo() {

    }
}

context(param: Context)
val check: Unit
    get() {
        <expr>param</expr>.foo()
    }

// LANGUAGE: +ContextParameters
