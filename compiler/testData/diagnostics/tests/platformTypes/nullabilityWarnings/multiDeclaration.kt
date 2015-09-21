// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    public interface Multi {
        String component1();
        String component2();
    }

    @NotNull
    public static Multi staticNN;
    @Nullable
    public static Multi staticN;
    public static Multi staticJ;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    val (<!OPERATOR_MODIFIER_REQUIRED!>a1<!>, <!OPERATOR_MODIFIER_REQUIRED!>b1<!>) = platformNN
    val (a2, b2) = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>platformN<!>
    val (<!OPERATOR_MODIFIER_REQUIRED!>a3<!>, <!OPERATOR_MODIFIER_REQUIRED!>b3<!>) = platformJ
}