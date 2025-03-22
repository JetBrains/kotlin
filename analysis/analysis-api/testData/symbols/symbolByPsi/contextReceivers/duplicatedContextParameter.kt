// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +ContextParameters

context(c: Int)
context(c: String)
fun bar() {}

context(c: Int)
context(c: String)
val foo: Int get() = 0
