class Context {
    fun foo() {

    }
}

context(param: Context)
fun check() {
    <expr>param</expr>.foo()
}

// LANGUAGE: +ContextParameters
