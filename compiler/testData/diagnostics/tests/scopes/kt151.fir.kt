//KT-151 Inherit visibility when overriding
package kt151

open class A {
    protected open fun x() {}
}

class B : A() {
    override fun x() {} // No visibility modifier required
}

fun test(b: B) {
    b.<!HIDDEN!>x<!>()
}


//more tests
open class C {
    internal open fun foo() {}
}

interface T {
    fun foo() {}
}

class D : C(), T {
    protected override fun foo() {}
}

class E : C(), T {
    internal override fun foo() {}
}

class F : C(), T {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>override<!> fun foo() {}
}

class G : C(), T {
    public override fun foo() {}
}
