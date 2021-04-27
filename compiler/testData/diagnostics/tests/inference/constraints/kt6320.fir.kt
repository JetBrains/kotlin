// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// AssertionError in ConstraintSystem(The constraint shouldn't contain different type variables on both sides: Y <: X)

class A<X, Y : X>

class B<X, Y : X>(foo: A<X, Y>) {
    fun test1(a: A<X, Y>) {
        B(a)
        val b: B<X, Y> = B(a)
        // crash here
    }
}

class C<X, Z, Y : X>

class D<X, Z, Y : X>(foo: C<X, Z, Y>) {
    fun test(a: C<Y, Y, Y>) {
        val d: D<X, Y, Y> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>D(a)<!>
    }
}
