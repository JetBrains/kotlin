//KT-151 Inherit visibility when overriding
package kt151

open class A {
    protected open fun x() {}
}

class B : A() {
    override fun x() {} // No visibility modifier required
}

fun test(b: B) {
    b.<!INVISIBLE_MEMBER!>x<!>()
}


//more tests
open class C {
    internal open fun foo() {}
}

interface T {
    protected fun foo() {}
}

class D : C(), T {
    <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>protected<!> override fun foo() {}
}

class E : C(), T {
    <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>internal<!> override fun foo() {}
}

class F : C(), T {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>private<!> override fun foo() {}
}

class G : C(), T {
    public override fun foo() {}
}