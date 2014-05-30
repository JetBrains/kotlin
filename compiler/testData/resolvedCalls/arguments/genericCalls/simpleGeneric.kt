// !CALL: foo

class A {}

fun <T> foo(t: T) {}

fun bar() {
    foo(A())
}