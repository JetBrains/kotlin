context(int: Int, string: String)
fun foo() {

}

context(c: Int)
fun Boolean.usage() {
    <expr>foo()</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS