// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: B() = ArgumentMatch(b : B, SUCCESS)
// !ARG_2: A() = ArgumentMatch(a : A, SUCCESS)

class A {}
class B {}

fun foo(a: A, b: B) {}

fun bar() {
    foo(b = B(), a = A())
}