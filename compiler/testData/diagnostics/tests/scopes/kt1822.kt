//KT-1822 Error 'cannot infer visibility' required
package kt1822

open class C {
    internal open fun foo() {}
}

interface T {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() {}
}

class G : C(), T {
    <!CANNOT_INFER_VISIBILITY!>override fun foo()<!> {} //should be an error "cannot infer visibility"; for now 'public' is inferred in such cases
}

open class A {
    internal open fun foo() {}
}

interface B {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() {}
}

interface D {
    public fun foo() {}
}

class E : A(), B, D {
    override fun foo() {}
}