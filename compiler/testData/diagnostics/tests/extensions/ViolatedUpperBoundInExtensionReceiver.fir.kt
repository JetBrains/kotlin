// LANGUAGE: +ContextReceivers
interface I<T: Number>

fun I<String>.foo() {}

context(I<String>)
fun bar() {}
