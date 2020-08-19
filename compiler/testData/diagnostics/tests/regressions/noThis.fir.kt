interface A { fun f() }

open class P(val z: B)

class B : A {
    override fun f() {}
    class C : A by this {}
    class D(val x : B = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>)
    class E : P(this)
}