// "Add 'abstract fun f()' to 'A'" "true"
abstract class A {
}
class B : A() {
    <caret>override fun f() {}
}