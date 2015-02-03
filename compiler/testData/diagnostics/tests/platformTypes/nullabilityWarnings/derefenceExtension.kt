// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

import p.*

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    platformNN.foo()
    platformN.foo()
    platformJ.foo()

    platformNN.bar()
    platformN.bar()
    platformJ.bar()
}

fun J.foo() {}
fun J?.bar() {}