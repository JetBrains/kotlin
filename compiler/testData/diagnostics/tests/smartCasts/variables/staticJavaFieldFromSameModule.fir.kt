// SKIP_TXT

// FILE: J.java
public class J {
    public static final Object staticFinalJava = "";
    public static Object staticNonFinalJava = "";
}

// FILE: Test.kt
fun isCast() {
    if (J.staticFinalJava is String) {
        J.staticFinalJava.length
        (J.staticFinalJava <!USELESS_CAST!>as String<!>).length
    }

    if (J.staticNonFinalJava is String) {
        <!SMARTCAST_IMPOSSIBLE!>J.staticNonFinalJava<!>.length
        (J.staticFinalJava as String).length
    }
}

fun asCast() {
    J.staticFinalJava as String
    J.staticFinalJava.length

    J.staticNonFinalJava as String
    <!SMARTCAST_IMPOSSIBLE!>J.staticNonFinalJava<!>.length
}
