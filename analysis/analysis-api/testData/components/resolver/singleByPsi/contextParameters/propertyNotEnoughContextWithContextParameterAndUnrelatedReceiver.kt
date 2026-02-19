context(int: Int, string: String)
val foo: Boolean get() = false

context(c: Int)
fun Boolean.usage() {
    <expr>foo</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS