// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

fun test() {
    foo(J.staticNN)
    foo(<!TYPE_MISMATCH!>J.staticN<!>)
    foo(J.staticJ)

    bar(J.staticNN)
    bar(J.staticN)
    bar(J.staticJ)
}

fun foo(j: J) {}
fun bar(j: J?) {}