// !DIAGNOSTICS: -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

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
    foo(J.staticNN)
    foo(J.staticN)
    foo(J.staticJ)

    bar(J.staticNN)
    bar(J.staticN)
    bar(J.staticJ)
}

fun foo(j: J) {}
fun bar(j: J?) {}
