class A
class B

context(_: A, _: B)
fun usage() = 2

context(a: A)
fun <T1> B.foo(param: Int = <expr>usage()</expr>, param2: String) {

}

// LANGUAGE: +ContextParameters