context(param1: Int, param2: String)
fun foo(b: Boolean) {

}

context(param1: Int, param2: String)
fun usage() {
    <expr>::foo</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS
