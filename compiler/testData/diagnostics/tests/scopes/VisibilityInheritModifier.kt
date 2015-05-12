package b

open class A {
    internal open fun foo() {}
}

class B : A() {
    <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>protected<!> override fun foo() {}
}

class C : A() {
    internal override fun foo() {}
}

//------------
open class D {
    private open fun self() : D = this
}

class E : D() {
    internal <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun self() = this

    fun test() {
        val <!UNUSED_VARIABLE!>s<!> : E = self()
    }
}

//------------
open class F {
    protected open fun protected_fun() {}
}

class G : F() {
    override fun protected_fun() {}
}

fun test_fun_stays_protected(g: G) {
    g.<!INVISIBLE_MEMBER!>protected_fun<!>()
}

//------------
open class H {
    protected open fun pi_fun() {}
}

class I : H() {
    protected override fun pi_fun() {}
}

class J : H() {
    <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>internal<!> override fun pi_fun() {}
}

class K : H() {
    public override fun pi_fun() {}
}

//-------------
interface T {
    public fun foo() {}
}

open class L : T {
    override fun foo() {}
}

class M : L() {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!> override fun foo() {}
}
//---------------
interface R {
    <!INCOMPATIBLE_MODIFIERS!>internal<!> <!INCOMPATIBLE_MODIFIERS!>protected<!> fun foo() {}
}

interface P : R {
    <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>internal<!> override fun foo() {}
}

interface Q : R {
    protected override fun foo() {}
}

class S : P, Q {
    <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>internal<!> override fun foo() {}
}