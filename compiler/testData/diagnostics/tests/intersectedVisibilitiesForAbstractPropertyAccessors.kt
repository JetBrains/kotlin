// FIR_IDENTICAL
// ISSUE: KT-66046

abstract class I1 {
    abstract var a: Int
        protected set
}

interface I2 {
    var a: Int
}

abstract class C : I1(), I2

abstract class I3 {
    protected abstract fun foo(): Int
}

interface I4 {
    fun foo(): Int
}

abstract class B : I3(), I4
