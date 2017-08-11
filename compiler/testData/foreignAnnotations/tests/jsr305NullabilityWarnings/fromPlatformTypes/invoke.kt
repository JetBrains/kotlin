// WARNING_FOR_JSR305_ANNOTATIONS

// FILE: J.java

public class J {
    public interface Invoke {
        void invoke();
    }

    @MyNonnull
    public static Invoke staticNN;
    @MyNullable
    public static Invoke staticN;
    public static Invoke staticJ;
}

// FILE: k.kt

fun test() {
    J.staticNN()
    J.<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>staticN<!>()
    J.staticJ()
}