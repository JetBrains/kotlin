context(s: String,)
fun foo() {}

context(
    s: String,
    t: Int,
)
fun foo() {}

context(s: String,)
val foo: String get() = ""

context(
    s: String,
    t: Int,
)
val foo: String get() = ""
// LANGUAGE: +ContextParameters
