// "Add 'abstract fun f()' to 'A'" "true"
sealed class A {
}
class B : A() {
    <caret>override fun f() {}
}