context(int: Int, string: String)
fun foo() {

}

fun Int.usage() {
    <expr>foo()</expr>
}

// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS