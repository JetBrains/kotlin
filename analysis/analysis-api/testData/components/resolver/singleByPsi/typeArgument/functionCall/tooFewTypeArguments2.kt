// COMPILATION_ERRORS
fun <A, B, C> generic(a: A, b: B, c: C) { }

fun foo() {
    generic<<caret>String, String>("a", "b", "c")
}
