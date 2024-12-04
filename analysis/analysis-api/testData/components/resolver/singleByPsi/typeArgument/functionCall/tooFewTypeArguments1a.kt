// COMPILATION_ERRORS
interface X

fun <A, B, C> generic() { }

fun foo() {
    generic<String, <caret>X>()
}
