context(int: Int, string: String)
fun foo() {

}

context(Int)
fun usage() {
    <expr>foo()</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS