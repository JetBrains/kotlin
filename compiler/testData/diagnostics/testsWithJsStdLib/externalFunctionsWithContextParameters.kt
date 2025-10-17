// LANGUAGE: +ContextParameters
external class Scope1
class Scope2

context(scope1: Scope1, scope2: Scope2)
external fun foo()

context(scope1: Scope1, scope2: Scope2)
fun bar() {
    foo()
}

external fun <A, B, R> context(a: A, b: B, block: context(A, B) () -> R): R

fun baz(scope1: Scope1, scope2: Scope2) {
    context(scope1, scope2) {
        foo()
    }
}
