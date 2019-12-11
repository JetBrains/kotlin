// FILE: J.java

class J {
    static String foo() { return "abc"; }
}

// FILE: test.kt

fun bar() {
    var v: String?
    v = J.foo()
    v.length
    gav(v)
}

fun gav(v: String) = v