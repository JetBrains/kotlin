// COMPILATION_ERRORS
interface X

fun <A> generic(a: A) { }

fun foo() {
    generic<Int, <caret>X>(5)
}
