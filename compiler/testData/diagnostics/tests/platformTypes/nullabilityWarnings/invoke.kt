// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    public interface Invoke {
        void invoke();
    }

    @NotNull
    public static Invoke staticNN;
    @Nullable
    public static Invoke staticN;
    public static Invoke staticJ;
}

// FILE: k.kt

import p.*

fun test() {
    J.staticNN()
    J.<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>staticN<!>()
    J.staticJ()
}

