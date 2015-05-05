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
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    platformNN.foo()
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>.foo()
    platformJ.foo()

    with(platformNN) {
        foo()
    }
    with(platformN) {
        <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>foo<!>()
    }
    with(platformJ) {
        foo()
    }

    platformNN.bar()
    platformN.bar()
    platformJ.bar()
}

fun J.foo() {}
fun J?.bar() {}
fun <T> with(t: T, f: T.() -> Unit) {}
