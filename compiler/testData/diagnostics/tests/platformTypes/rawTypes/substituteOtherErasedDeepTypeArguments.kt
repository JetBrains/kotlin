// WITH_STDLIB
// FULL_JDK

// FILE: X.java
class X<B extends I1<P, P, P>, P, A extends I1<I2<B>, B, I1<P, I2<B>, P>>> {
    static final E<X> E = new E<>();

    String getId() {
        return null;
    }
}

// FILE: E.java
class E<T> {
    T getT() {
        return null;
    }
}

// FILE: I1.java
interface I1<P, A, F> {}

// FILE: I2.java
interface I2<S> {}

// FILE: test.kt
fun test() {
    val t = X.E.t
    <!DEBUG_INFO_EXPRESSION_TYPE("(X<(I1<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>..I1<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>?), (kotlin.Any..kotlin.Any?), (I1<*, (I1<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>..I1<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>?), *>..I1<*, (I1<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>..I1<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>?), *>?)>..X<out (I1<*, *, *>..I1<*, *, *>?), *, out (I1<*, out (I1<*, *, *>..I1<*, *, *>?), *>..I1<*, out (I1<*, *, *>..I1<*, *, *>?), *>?)>?)")!>t<!>
    t.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>id<!> // error before
}
