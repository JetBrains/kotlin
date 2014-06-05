class A {}

fun foo(a: A) {}

fun bar() {
    <caret>foo(A())
}