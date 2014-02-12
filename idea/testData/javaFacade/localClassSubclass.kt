class A
fun foo() {
    trait Z: A {}
    fun bar() {
        class <caret>O2: Z {}
    }
}