interface IA {
    fun foo(): Number
}

interface IB : IA {
    override fun foo(): Int
}

object AImpl : IA {
    override fun foo() = 42
}

open class C : IA by AImpl, IB

class D : C() {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Double<!> = 3.14
}
