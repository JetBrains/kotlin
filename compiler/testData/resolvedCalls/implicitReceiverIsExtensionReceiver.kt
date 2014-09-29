class A {}

fun A.foo() {}

fun A.bar() {
    <caret>foo()
}
