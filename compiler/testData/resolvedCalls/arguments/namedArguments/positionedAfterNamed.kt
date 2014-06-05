class A {}
class B {}

fun foo(a: A, b: B) {}

fun bar() {
    <caret>foo(b = B(), A())
}