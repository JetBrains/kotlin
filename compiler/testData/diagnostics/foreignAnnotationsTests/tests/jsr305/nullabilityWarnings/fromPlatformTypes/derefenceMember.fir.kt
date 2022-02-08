// !DIAGNOSTICS: -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
public class J {
    @MyNonnull
    public static J staticNN;
    @MyNullable
    public static J staticN;
    public static J staticJ;

    public void foo() {}
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
    platformN.foo()
    platformJ.foo()

    with(platformNN) {
        foo()
    }
    with(platformN) {
        foo()
    }
    with(platformJ) {
        foo()
    }
}
