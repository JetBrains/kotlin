// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN

    foo(platformNN <!USELESS_ELVIS!>?: ""<!>)

    val bar = Bar()
    bar(platformNN <!USELESS_ELVIS!>?: ""<!>)
}

fun foo(a: Any) {}

class Bar {
    operator fun invoke(a: Any) {}
}