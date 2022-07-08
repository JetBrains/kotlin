class B {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val a = A()<!>
    val b: Boolean = false

    fun foo() = a.a.b

    inner class A(l: Int) {

        val a: B = this@B

        constructor(b: Boolean = foo()) : this(if (foo()) 5 else 4) {
            this@B.a
        }
    }
}