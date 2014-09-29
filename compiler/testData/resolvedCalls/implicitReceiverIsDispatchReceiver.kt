class A {
    fun foo() {}
}

fun A.bar() {
    <caret>foo()
}
