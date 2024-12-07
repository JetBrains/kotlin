// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
abstract class A {
    abstract var i: Int
}

class B() : A() {
    override var i = 1
}
