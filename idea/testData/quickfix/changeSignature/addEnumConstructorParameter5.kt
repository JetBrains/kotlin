// "Add parameter to constructor 'Foo'" "true"
// DISABLE-ERRORS
enum class Foo {
    A(1<caret>),
    B(2),
    C(),
    D,
}
