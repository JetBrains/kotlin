// WARNING_FOR_JSR305_ANNOTATIONS

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
class B : List<String> by <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>
class C : List<String> by J.staticJ