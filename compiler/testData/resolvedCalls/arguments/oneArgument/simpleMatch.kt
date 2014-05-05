// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: A() = ArgumentMatch(a : A, SUCCESS)

class A {}

fun foo(a: A) {}

fun bar() {
    foo(A())
}