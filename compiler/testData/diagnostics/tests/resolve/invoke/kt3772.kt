// FIR_IDENTICAL
//KT-3772 Invoke and overload resolution ambiguity
package bar

open class A {
    public operator fun invoke(f: A.() -> Unit) {}
}

class B {
    public operator fun invoke(f: B.() -> Unit) {}
}

open class C
val C.attr: A
    get() = A()

open class D: C()
val D.attr: B
    get() = B()


fun main() {
    val b =  D()
    b.attr {} // overload resolution ambiguity

    val d = b.attr
    d {}      // no error
}
