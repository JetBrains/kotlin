// FIR_IDENTICAL
abstract class A : Function1<Any, Unit>

abstract class B : (Int)->Unit

// Named parameter is prohibited because of possible inconsistency between
// type declaration and actual override
class C : (<!UNSUPPORTED!>x<!>: Int)->Unit {
    override fun invoke(p1: Int): Unit {}
}

class D : (<!UNSUPPORTED!>Int<!>: Int)->Unit {
    override fun invoke(p1: Int): Unit {}
}


class E : ((k: String) -> Unit) -> Unit {
    override fun invoke(p1: (k: String) -> Unit) {
    }
}
