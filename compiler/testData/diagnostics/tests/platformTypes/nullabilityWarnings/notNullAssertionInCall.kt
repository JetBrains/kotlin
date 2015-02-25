// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
}

// FILE: k.kt

import p.*

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN

    foo(platformNN<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    val bar = Bar()
    bar(platformNN<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}

fun foo(a: Any) {}

class Bar {
    fun invoke(a: Any) {}
}