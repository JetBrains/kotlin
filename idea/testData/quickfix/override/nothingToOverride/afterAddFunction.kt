// "Add 'open fun f()' to 'A'" "true"
open class A {
    open fun f() {
    }
}
class B : A() {
    <caret>override fun f() {}
}