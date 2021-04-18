// FIR_IDENTICAL
abstract class A : I1 {
    open fun a(){}
}

interface I1 {
    fun i1()
    fun i()
}

interface I2 {
    fun i2()
    fun a()
}

interface I3 {
    fun i()
}

abstract class B : I2, A(), I3 {
    <caret>
}
