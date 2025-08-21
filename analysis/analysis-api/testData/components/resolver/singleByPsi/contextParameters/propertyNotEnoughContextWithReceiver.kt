context(int: Int, string: String)
val foo: Boolean get() = false

fun Int.usage() {
    <expr>foo</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS