// !DIAGNOSTICS: -UNUSED_VARIABLE
// WARNING_FOR_JSR305_ANNOTATIONS

// FILE: J.java

public class J {
    public interface Multi {
        String component1();
        String component2();
    }

    @MyNonnull
    public static Multi staticNN;
    @MyNullable
    public static Multi staticN;
    public static Multi staticJ;
}

// FILE: k.kt

fun test() {
    val platformNN = J.staticNN
    val platformN = J.staticN
    val platformJ = J.staticJ

    val (a1, b1) = platformNN
    val (a2, b2) = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>
    val (a3, b3) = platformJ
}