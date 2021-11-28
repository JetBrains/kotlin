// WITH_STDLIB
// FULL_JDK

// FILE: X.java
class X<B extends I1<P, P, P>, P, A extends I1<I2<B>, B, I1<P, I2<B>, P>>> {
    static final E<X> E = new E<>();

    String getId() {
        return null;
    }
}

class E<T> {
    T getT() {
        return null;
    }
}

interface I1<P, A, F> {}
interface I2<S> {}

// FILE: test.kt
fun test() {
    val t = X.E.<!UNRESOLVED_REFERENCE!>t<!>
    t
    t.<!UNRESOLVED_REFERENCE!>id<!> // error before
}
