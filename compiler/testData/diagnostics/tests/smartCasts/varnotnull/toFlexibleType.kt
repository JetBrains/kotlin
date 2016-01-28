// FILE: J.java

class J {
    static String foo() { return "abc"; }
}

// FILE: test.kt

fun bar() {
    var v: String?
    v = J.foo()
    <!DEBUG_INFO_SMARTCAST!>v<!>.length
    gav(<!DEBUG_INFO_SMARTCAST!>v<!>)
}

fun gav(v: String) = v