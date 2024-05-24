// COMPILATION_ERRORS
fun <A : Number> generic(a: A) { }

fun foo() {
    generic<<caret>Int, String>(2L)
}
