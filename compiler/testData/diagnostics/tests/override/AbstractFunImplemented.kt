// FIR_IDENTICAL
abstract class A {
    abstract fun foo(): Int
}

class B() : A() {
    override fun foo() = 1
}
