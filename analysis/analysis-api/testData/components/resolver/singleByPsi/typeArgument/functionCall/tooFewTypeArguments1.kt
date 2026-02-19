// COMPILATION_ERRORS
fun <A, B, C> generic() { }

fun foo() {
    generic<<caret>String, String>()
}
