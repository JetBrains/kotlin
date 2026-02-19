context(int: Int, string: String)
val foo: Boolean get() = false

context(contextParameter: String)
fun Int.usage() {
    <expr>foo</expr>
}

// LANGUAGE: +ContextParameters