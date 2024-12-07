// FIR_IDENTICAL
// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
import java.util.*;

public class J {
    @MyNonnull
    public static List<String> staticNN;
    @MyNullable
    public static List<String> staticN;
    public static List<String> staticJ;
}

// FILE: k.kt
fun test() {
    val platformNN = J.staticNN
    val platformN = J.staticN
    val platformJ = J.staticJ

    for (x in platformNN) {}
    for (x in <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>) {}
    for (x in platformJ) {}
}
