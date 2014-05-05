// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: b = ArgumentMatch(a : A, ARGUMENT_HAS_NO_TYPE)

class A {}

fun foo(a: A) {}

fun bar() {
    foo(b)
}