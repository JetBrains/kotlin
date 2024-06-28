// FIR_IDENTICAL
// ISSUE: KT-64841

open class A<X : A<X>>(a: Any?) {
    // Constraints while resolving super constructor call
    //
    // The signature of constructor comes substituted
    // constructor<X : R|A<A.B<X>>|>(a: R|kotlin/Any?|): R|A<A.B<X>>|
    //
    // Xv <: A<A<Xv>.B> (from type parameter bounds, actually it's the incorrect place)
    //
    // Xv := A<X>.B (from explicit type argument of constructor call)
    //  A<X>.B> <: A<A<Xv>.B>
    //  A<A<X>.B> <: A<A<Xv>.B>
    //  Xv := X
    //    X != A<X>.B -> FAIL
    inner class B : A<B>("") {
        fun foo() {
            A<B>("")
        }
    }
}
