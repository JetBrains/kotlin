// COMPILATION_ERRORS
interface X

fun <A> generic(a: A) { }

fun foo() {
    generic<<caret>Int, X>(5)
}
