context(int: Int, string: String)
fun foo() {

}

context(contextParameter: String)
fun Int.usage() {
    <expr>foo()</expr>
}

// LANGUAGE: +ContextParameters