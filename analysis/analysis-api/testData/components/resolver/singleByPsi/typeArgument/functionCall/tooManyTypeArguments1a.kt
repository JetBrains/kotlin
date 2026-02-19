// COMPILATION_ERRORS
interface X
interface Y

fun <A> generic() { }

fun foo() {
    generic<<caret>X, Y>()
}
