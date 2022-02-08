// WITH_STDLIB
// FULL_JDK

// FILE: X.java
class X<B extends I<B>> {
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

interface I<P> {}

// FILE: test.kt
fun test() {
    val t = X.E.<!UNRESOLVED_REFERENCE!>t<!>
    t
    t.<!UNRESOLVED_REFERENCE!>id<!> // should be OK
}
