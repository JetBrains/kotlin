// "Add 'open fun f(): Int' to 'A'" "true"
open class A {
    open fun f(): Int {
        throw UnsupportedOperationException()
    }
}
class B : A() {
    <caret>override fun f(): Int = 5
}