// "Create class 'Foo'" "true"
// ERROR: No value passed for parameter n

open class A(n: Int)

fun test(): A = <caret>Foo(2, "2")