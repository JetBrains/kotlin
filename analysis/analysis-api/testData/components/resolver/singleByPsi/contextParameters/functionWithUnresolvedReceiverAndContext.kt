context(string: String)
fun foo() {

}

context(contextParameter: String)
fun AAA.usage() {
    <expr>foo()</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS
// IGNORE_STABILITY_K1: candidates