// MODULE: original
fun foo() {
    fun bar () {}
}

// MODULE: copy
// COMPILATION_ERRORS
fun foo() {
    funbar () {}
}