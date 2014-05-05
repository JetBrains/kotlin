// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: "" = ArgumentMatch(a : A, TYPE_MISMATCH)

class A {}

fun foo(a: A) {}

fun bar() {
    foo("")
}