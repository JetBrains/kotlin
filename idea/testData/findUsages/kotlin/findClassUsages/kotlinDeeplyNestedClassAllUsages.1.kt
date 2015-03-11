package b

import a.Outer.Inner

public class X(bar: String? = Inner.A.bar): Inner.A() {
    var next: Inner.A? = Inner.A()
    val myBar: String? = Inner.A.bar

    {
        Inner.A.bar = ""
        Inner.A.foo()
    }

    fun foo(a: Inner.A) {
        val aa: Inner.A = a
        aa.bar = ""
    }

    fun getNext(): Inner.A? {
        return next
    }

    public override fun foo() {
        super<Inner.A>.foo()
    }

    default object: Inner.A() {

    }
}

object O: Inner.A() {

}

fun X.bar(a: Inner.A = Inner.A()) {

}

fun Any.toA(): Inner.A? {
    return if (this is Inner.A) this as Inner.A else null
}