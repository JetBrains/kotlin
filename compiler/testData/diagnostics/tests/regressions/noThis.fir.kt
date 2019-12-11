interface A { fun f() }

open class P(val z: B)

class B : A {
    override fun f() {}
    class C : A by this {}
    class D(val x : B = this)
    class E : <!INAPPLICABLE_CANDIDATE!>P<!>(this)
}