// FIR_IDENTICAL
// !DIAGNOSTICS: -SENSELESS_COMPARISON

// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
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

    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>platformNN<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!>
    platformN?.foo()
    platformJ?.foo()

    if (platformNN != null) {
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>platformNN<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!>
    }

    if (platformN != null) {
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>platformN<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!>
    }

    if (platformJ != null) {
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>platformJ<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!>
    }
}

