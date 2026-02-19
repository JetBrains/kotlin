context(Int, String)
val foo: Boolean get() = false

fun usage() {
    ::<expr>foo</expr>
}

// LANGUAGE: +ContextReceivers
// COMPILATION_ERRORS