package b

import a.Outer

public class X(bar: String? = Outer.A.bar): Outer.A() {
    var next: Outer.A? = Outer.A()
    val myBar: String? = Outer.A.bar

    {
        Outer.A.bar = ""
        Outer.A.foo()
    }

    fun foo(a: Outer.A) {
        val aa: Outer.A = a
        aa.bar = ""
    }

    fun getNext(): Outer.A? {
        return next
    }

    public override fun foo() {
        super<Outer.A>.foo()
    }

    default object: Outer.A() {

    }
}

object O: Outer.A() {

}

fun X.bar(a: Outer.A = Outer.A()) {

}

fun Any.toA(): Outer.A? {
    return if (this is Outer.A) this as Outer.A else null
}