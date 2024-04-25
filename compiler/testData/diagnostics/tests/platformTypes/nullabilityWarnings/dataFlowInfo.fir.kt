// DIAGNOSTICS: -UNUSED_PARAMETER
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
    foo(<!ARGUMENT_TYPE_MISMATCH!>n<!>)
    J.staticNN = <!ASSIGNMENT_TYPE_MISMATCH!>n<!>
    if (n != null) {
        foo(n)
        J.staticNN = n
    }

    val x: J? = null
    J.staticNN = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
    if (x != null) {
        J.staticNN = x
    }
}

fun foo(j: J) {}
