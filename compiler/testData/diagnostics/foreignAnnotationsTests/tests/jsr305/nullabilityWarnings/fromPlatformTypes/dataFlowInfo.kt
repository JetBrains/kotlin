// DIAGNOSTICS: -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
public class J {
    @MyNonnull
    public static J staticNN;
    @MyNullable
    public static J staticN;
}

// FILE: k.kt
fun test() {
    val n = J.staticN
    foo(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>n<!>)
    J.staticNN = <!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>n<!>
    if (n != null) {
        foo(n)
        J.staticNN = n
    }

    val x: J? = null
    J.staticNN = <!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x<!>
    if (x != null) {
        J.staticNN = x
    }
}

fun foo(j: J) {}
