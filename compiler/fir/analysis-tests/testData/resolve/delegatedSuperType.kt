// FIR_DISABLE_LAZY_RESOLVE_CHECKS
interface A {
    fun foo()
}

class B : A {
    override fun foo() {}
}

class C(val b: B) : A by b
