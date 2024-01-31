// FIR_IDENTICAL
// SKIP_TXT

// MODULE: lib
// FILE: J.java
public class J {
    public static final Object staticFinalJava = "";
    public static Object staticNonFinalJava = "";
}

// MODULE: app(lib)
fun isCast() {
    if (J.staticFinalJava is String) {
        <!SMARTCAST_IMPOSSIBLE!>J.staticFinalJava<!>.length
        (J.staticFinalJava as String).length
    }

    if (J.staticNonFinalJava is String) {
        <!SMARTCAST_IMPOSSIBLE!>J.staticNonFinalJava<!>.length
        (J.staticFinalJava as String).length
    }
}

fun asCast() {
    J.staticFinalJava as String
    <!SMARTCAST_IMPOSSIBLE!>J.staticFinalJava<!>.length

    J.staticNonFinalJava as String
    <!SMARTCAST_IMPOSSIBLE!>J.staticNonFinalJava<!>.length
}
