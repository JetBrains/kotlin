// "Add 'open fun f(): Int' to 'A'" "true"
open class A {
}
class B : A() {
    <caret>override fun f(): Int = 5
}