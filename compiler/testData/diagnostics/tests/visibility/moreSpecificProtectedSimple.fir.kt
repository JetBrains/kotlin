// SKIP_TXT
// FIR_DUMP
// ISSUE: KT-56310

interface Base
interface Derived : Base

interface M1 {
    val success: Boolean
}
interface M1Sub : M1

open class A {
    open protected fun baz(a: Derived): M1 = TODO()
    open protected fun foo(a: Derived): M1 = TODO()

    fun f(a: A, b: B, d: Derived) {
        a.baz(d).success // OK in K1 and K2
        a.foo(d).success // OK in K1 and K2

        // Both K1 and K2 resolves calls to B's members with String return type because other's members in B type are invisible
        b.baz(d).length
        b.foo(d).length

        when (a) {
            is B -> {
                // OK in K1 because `a.baz(d)` resolved to String returning methid (just as `b.baz(d)`)
                a.baz(d).<!UNRESOLVED_REFERENCE!>length<!>
                // OK in K2, because we have two visible candidates:
                // - A::baz from original (after unwrapping smart cast) receiver
                // - B::baz that returns String
                // But the first one is more specific via its parameter types
                a.baz(d).success // Unresolved reference in K1, going to be OK in K2

                // Works in K2 for the same reasons as `a.baz(d)`
                // In K1, works by coincidence because we bind override groups for members from original and smart cast receiver,
                // and as return type from B is the same (not more specific), we choose the member from A as a group representative.
                // Thus, we have successful candidates
                // - A::foo
                // - B::foo returning String
                // But A::foo is more specific, so we choose it
                a.foo(d).success
            }
        }
    }
}

class B : A() {
    override fun baz(a: Derived): M1Sub = TODO()
    public fun baz(a: Base): String = TODO()

    // The only difference between `baz` and `foo` is that the former has more specific covariant return type (M1Sub)
    override fun foo(a: Derived): M1 = TODO()
    public fun foo(a: Base): String = TODO()
}
