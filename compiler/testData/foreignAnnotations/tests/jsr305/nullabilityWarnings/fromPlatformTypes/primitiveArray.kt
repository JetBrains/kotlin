// JSR305_GLOBAL_REPORT warn

// FILE: J.java

public class J {
    @MyNonnull
    public static int[] staticNN;
    @MyNullable
    public static int[] staticN;
    public static int[] staticJ;
}

// FILE: k.kt

fun test() {
    val platformNN = J.staticNN
    val platformN = J.staticN
    val platformJ = J.staticJ

    platformNN[0]
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>[0]
    platformJ[0]

    platformNN[0] = 1
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>[0]  = 1
    platformJ[0]  = 1
}

