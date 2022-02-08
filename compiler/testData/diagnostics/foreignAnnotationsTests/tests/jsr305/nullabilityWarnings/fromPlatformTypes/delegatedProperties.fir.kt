// JSR305_GLOBAL_REPORT: warn

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
var B by J.staticN
var C by J.staticJ
