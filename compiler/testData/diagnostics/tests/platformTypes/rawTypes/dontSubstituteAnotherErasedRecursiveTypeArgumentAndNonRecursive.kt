// WITH_STDLIB
// FULL_JDK

// FILE: X.java
class X<B extends I<P, B>, P> {
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

interface I<P, L> {}

// FILE: test.kt
fun test() {
    val t = X.E.t
    <!DEBUG_INFO_EXPRESSION_TYPE("(X<*, (kotlin.Any..kotlin.Any?)>..X<*, *>?)")!>t<!>
    t.id // should be OK
}
