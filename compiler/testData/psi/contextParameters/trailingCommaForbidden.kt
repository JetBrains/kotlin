// COMPILATION_ERRORS

context(s: String,,)
fun foo() {}

context(
    s: String,,
)
fun foo() {}

context(
    s: String
,,
)
fun foo() {}

context(,)
fun foo() {}

context(, s: String)
fun foo() {}
