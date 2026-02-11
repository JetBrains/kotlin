context(Int, String)
val foo: Boolean get() = false

fun usage() {
    <expr>::foo</expr>
}

// LANGUAGE: +ContextReceivers, -ContextParameters
// COMPILATION_ERRORS