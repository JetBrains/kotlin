// !WITH_NEW_INFERENCE
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static Exception staticNN;
    @Nullable
    public static Exception staticN;
    public static Exception staticJ;
}

// FILE: k.kt

fun test() {
    throw J.staticNN
}

fun test1() {
    throw <!TYPE_MISMATCH!>J.<!NI;TYPE_MISMATCH!>staticN<!><!>
}

fun test2() {
    throw J.staticJ
}