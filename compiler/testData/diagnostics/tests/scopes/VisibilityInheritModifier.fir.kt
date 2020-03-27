package b

open class A {
    internal open fun foo() {}
}

class B : A() {
    protected override fun foo() {}
}

class C : A() {
    internal override fun foo() {}
}

//------------
open class D {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun self() : D = this
}

class E : D() {
    internal override fun self() = this

    fun test() {
        val s : E = self()
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
    g.protected_fun()
}

//------------
open class H {
    protected open fun pi_fun() {}
}

class I : H() {
    protected override fun pi_fun() {}
}

class J : H() {
    internal override fun pi_fun() {}
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
    internal override fun foo() {}
}
//---------------
interface R {
    fun foo() {}
}

interface P : R {
    override fun foo() {}
}

interface Q : R {
    override fun foo() {}
}

class S : P, Q {
    internal override fun foo() {}
}