// FIR_DUMP

class A<in T>(t: T) {
    private val t: T = t  // PRIVATE_TO_THIS

    private val i: B = B()

    fun test() {
        val x: T = t      // Ok
        val y: T = this.t // Ok
    }

    fun foo(a: A<String>) {
        val x: String = a.<!INVISIBLE_MEMBER!>t<!> // Invisible!
    }

    fun bar(a: A<*>) {
        a.<!INVISIBLE_MEMBER!>t<!> // Invisible!
    }

    inner class B {
        fun baz(a: A<*>) {
            a.i
        }
    }
}
