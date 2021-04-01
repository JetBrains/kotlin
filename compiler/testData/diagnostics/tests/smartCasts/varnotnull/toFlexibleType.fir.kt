// FILE: J.java

class J {
    static String foo() { return "abc"; }
}

// FILE: test.kt

fun bar() {
    var v: String?
    v = J.foo()
    v<!UNSAFE_CALL!>.<!>length
    <!INAPPLICABLE_CANDIDATE!>gav<!>(v)
}

fun gav(v: String) = v
