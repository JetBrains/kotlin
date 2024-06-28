// FIR_IDENTICAL
// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
public class J {
    @MyNonnull
    public static Exception staticNN;
    @MyNullable
    public static Exception staticN;
    public static Exception staticJ;
}

// FILE: k.kt
fun test() {
    throw J.staticNN
}

fun test1() {
    throw <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>
}

fun test2() {
    throw J.staticJ
}
