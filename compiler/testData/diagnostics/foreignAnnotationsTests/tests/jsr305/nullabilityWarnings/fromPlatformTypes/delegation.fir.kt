// JSR305_GLOBAL_REPORT: warn
// FULL_JDK
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
class A : List<String> by J.staticNN
class B : List<String> by J.staticN
class C : List<String> by J.staticJ
