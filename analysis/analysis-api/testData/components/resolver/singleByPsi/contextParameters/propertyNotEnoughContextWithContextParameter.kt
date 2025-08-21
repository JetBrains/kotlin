context(int: Int, string: String)
val foo: Boolean get() = false

context(c: Int)
fun usage() {
    <expr>foo</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS