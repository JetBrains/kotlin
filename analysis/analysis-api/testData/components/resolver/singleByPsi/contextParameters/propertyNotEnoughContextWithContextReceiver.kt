context(int: Int, string: String)
val foo: Boolean get() = false

context(Int)
fun usage() {
    <expr>foo</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS