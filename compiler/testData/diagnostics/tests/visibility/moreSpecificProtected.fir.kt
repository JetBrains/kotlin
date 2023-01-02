// SKIP_TXT
// FIR_DUMP
// CHECK_TYPE

open class Base
class Derived : Base()

interface M1
interface M2
interface M2Sub : M2
interface M3
interface M3Sub : M3
interface M4


interface M5
interface M5Sub : M5
interface M5SubSub : M5Sub
interface M6

open class A {
    public fun foo(a1: Base, a2: Base): M1 = TODO()
    protected open fun foo(a1: Base, a2: Derived): M2 = TODO()
    protected open fun foo(a1: Derived, a2: Derived): M3  = TODO()

    public fun baz(a1: Base, a2: Base): M4 = TODO()
    protected open fun baz(a1: Derived, a2: Derived): M5 = TODO()
}

open class B : A() {
    public val fromB: Any = Any()

    override fun foo(a1: Base, a2: Derived): M2Sub = TODO()

    override fun baz(a1: Derived, a2: Derived): M5Sub = TODO()

    fun f(a: A, b: B, c: C, d: Derived) {
        // Only visible is M1
        a.foo(d, d) checkType { _<M1>() }
        // We may call M3 and it's the most specific by its parameters
        b.foo(d, d) checkType { _<M3>() }
        // We can't call M3 (and M3Sub) as it's protected overriden in C, so it's invisible because
        // we only allow to call protected members on dispatch receiver values that are subtypes of a dispatch receiver parameter
        // So, the next visible and specific member is M2Sub
        c.foo(d, d) checkType { _<M2Sub>() }

        // Only visible is M4
        a.baz(d, d) checkType { _<M4>() }
        // M5Sub is more specific and visible for `b` receiver
        b.baz(d, d) checkType { _<M5Sub>() }

        // M5SubSub is invisible because it's protected in something that is not our super-class
        // M6 and M1 are visible, but M6 is more specific
        c.baz(d, d) checkType { _<M6>() }

        when (a) {
            is C -> {
                // Make sure smart cast works
                a.fromC
                // The same logic as for `c.foo`
                a.foo(d, d) checkType { _<M2Sub>() }

                // The same logic as for `c.baz`
                a.baz(d, d) checkType { _<M6>() }
            }
            is B -> {
                // Make sure smart cast works
                a.fromB
                // The same logic as `b.foo`
                a.foo(d, d) checkType { _<M3>() }

                // The same logic as for `b.baz`
                a.baz(d, d) checkType { _<M5Sub>() }
            }
        }

        when (b) {
            is C -> {
                b.fromC
                // In K1, it works just as `c.foo`
                b.foo(d, d) checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><M2Sub>() }
                // In K2, M3Sub is invisible, but we have candidate M3 from original receiver and we choose it
                // Unlike the case of `c.foo` when we choose M2Sub because we don't have more special M3 there in the scope of C
                // (in the meaning of overload comparison by the value parameter types)
                b.foo(d, d) checkType { _<M3>() }

                // In K1, it works just as `c.foo`
                b.baz(d, d) checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><M6>() }
                // In K2, M5SubSub is invisible, but we have candidate M5Sub from original receiver and we choose it
                // Unlike the case of `c.baz` when we choose M6 because we don't have more special M5Sub there in the scope of C
                // (in the meaning of overload comparison by the value parameter types)
                b.baz(d, d) checkType { _<M5Sub>() }
            }
        }
    }
}

class C : B() {
    public val fromC: Any = Any()

    override fun foo(a1: Derived, a2: Derived): M3Sub = TODO()

    override fun baz(a1: Derived, a2: Derived): M5SubSub = TODO()
    // public fallback
    public fun baz(a1: Derived, a2: Base): M6 = TODO()
}
