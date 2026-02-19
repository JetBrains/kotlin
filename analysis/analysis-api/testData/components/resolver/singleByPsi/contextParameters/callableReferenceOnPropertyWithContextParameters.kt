context(param1: Int, param2: String)
val foo: Boolean get() = false

fun usage() {
    <expr>::foo</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS