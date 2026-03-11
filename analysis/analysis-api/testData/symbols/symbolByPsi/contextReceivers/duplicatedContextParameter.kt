// LANGUAGE: +ContextParameters

context(c: Int)
context(c: String)
fun bar() {}

context(c: Int)
context(c: String)
val foo: Int get() = 0
