//KT-3772 Invoke and overload resolution ambiguity
package bar

open class A {
    public fun invoke(<!UNUSED_PARAMETER!>f<!>: A.() -> Unit) {}
}

class B {
    public fun invoke(<!UNUSED_PARAMETER!>f<!>: B.() -> Unit) {}
}

open class C
val C.attr = A()

open class D: C()
val D.attr = B()


fun main(args: Array<String>) {
    val b =  D()
    b.attr {} // overload resolution ambiguity

    val d = b.attr
    d {}      // no error
}