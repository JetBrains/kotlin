package test

class C(private val b: A.B) {
    fun test() {
        A.X()

        A.Companion.Y()
        A.foo(A.bar)
        //1.extFoo(1.extBar) // conflict

        b.OuterY()
        b.outerFoo(b.outerBar)

        b.OuterY()
        b.outerFoo(b.outerBar)

        A.O.Y()
        A.O.foo(A.O.bar)

        with (A.O) {
            A.Companion.Y()
            foo(bar)
            1.extFoo(1.extBar)
        }
    }
}