// WARNING_FOR_JSR305_ANNOTATIONS

// FILE: J.java

public class J {

    public interface DP {
        String getValue(Object a, Object b);
        String setValue(Object a, Object b, Object c);
    }

    @MyNonnull
    public static DP staticNN;
    @MyNullable
    public static DP staticN;
    public static DP staticJ;
}

// FILE: k.kt

var A by J.staticNN
var B by <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>
var C by J.staticJ