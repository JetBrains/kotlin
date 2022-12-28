abstract class A : Function1<Any, Unit>

abstract class B : (Int)->Unit

// Named parameter is prohibited because of possible inconsistency between
// type declaration and actual override
class C : (x: Int)->Unit {
    override fun invoke(p1: Int): Unit {}
}