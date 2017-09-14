// !DIAGNOSTICS: -UNUSED_PARAMETER
// WARNING_FOR_JSR305_ANNOTATIONS

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
}
