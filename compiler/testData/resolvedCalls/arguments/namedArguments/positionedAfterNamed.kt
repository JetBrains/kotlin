// !CALL: foo

class A {}
class B {}

fun foo(a: A, b: B) {}

fun bar() {
    foo(b = B(), A())
}