// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: A() = ArgumentMatch(a : A, SUCCESS)
// !ARG_2: "" = ArgumentMatch(s : String, SUCCESS)

class A {}

fun foo(a: A) {}
fun foo(a: A, s: String) {}
fun foo(a: A, any: Any) {}

fun bar() {
    foo(A(), "")
}