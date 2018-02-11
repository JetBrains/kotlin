// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
}

// FILE: k.kt

fun test() {
    val n = J.staticN
    foo(<!TYPE_MISMATCH!>n<!>)
    J.staticNN = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>n<!>
    if (n != null) {
        foo(<!DEBUG_INFO_SMARTCAST!>n<!>)
        J.staticNN = <!DEBUG_INFO_SMARTCAST!>n<!>
    }

    val x: J? = null
    J.staticNN = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    if (x != null) {
        J.staticNN = <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

fun foo(j: J) {}