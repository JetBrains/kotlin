// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: A() = ArgumentMatch(t : A, SUCCESS)

class A {}

fun <T> foo(t: T) {}

fun bar() {
    foo(A())
}