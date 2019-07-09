// "Add parameter to constructor 'Foo'" "true"
// DISABLE-ERRORS
enum class Foo(n: Int) {
    A(1, 2<caret>),
    B(3),
    C(3, 4),
    D(),
    E
}