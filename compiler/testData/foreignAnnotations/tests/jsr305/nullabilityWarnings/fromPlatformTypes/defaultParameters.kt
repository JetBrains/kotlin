// !DIAGNOSTICS: -UNUSED_PARAMETER
// WARNING_FOR_JSR305_ANNOTATIONS

// FILE: J.java

public class J {
    @MyNonnull
    public static J staticNN;
    @MyNullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

fun test() {
    val platformNN = J.staticNN
    val platformN = J.staticN
    val platformJ = J.staticJ

    fun foo(p: J = platformNN, p1: J = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>, p2: J = platformJ) {}

    fun foo1(p: J? = platformNN, p1: J? = platformN, p2: J? = platformJ) {}
}