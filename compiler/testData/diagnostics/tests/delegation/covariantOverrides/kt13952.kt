// FIR_IDENTICAL
interface IA {
    fun foo(): Number
}

interface IB : IA {
    override fun foo(): Int
}

object AImpl : IA {
    override fun foo() = 42
}

open <!RETURN_TYPE_MISMATCH_BY_DELEGATION!>class C<!> : IA by AImpl, IB

class D : C() {
    override fun foo(): Double = 3.14
}
