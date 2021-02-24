// FIR_IDENTICAL
abstract class A {
    abstract var i: Int
}

class B() : A() {
    override var i = 1
}
