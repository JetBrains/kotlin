trait Base {
    val x: Any
}

class A : Base {
    override val x: String = ""
}

open class B : Base {
    override val x: Number = 1.0
}

class C : B() {
    override val x: Int = 42
}

fun test() {
    val base = Base::x
    base : KMemberProperty<Base, Any>
    base.get(A()) : Any
    <!TYPE_MISMATCH!>base.get(B())<!> : Number
    <!TYPE_MISMATCH!>base.get(C())<!> : Int

    val a = A::x
    a : KMemberProperty<A, String>
    a.get(A()) : String
    <!TYPE_MISMATCH!>a.get(<!TYPE_MISMATCH!>B()<!>)<!> : Number

    val b = B::x
    b : KMemberProperty<B, Number>
    <!TYPE_MISMATCH!>b.get(C())<!> : Int
}
