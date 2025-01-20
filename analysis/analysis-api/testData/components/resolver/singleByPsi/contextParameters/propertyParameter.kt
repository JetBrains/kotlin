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
// IGNORE_STABILITY_K2: candidates
