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

    foo(platformNN ?: "")

    val bar = Bar()
    bar(platformNN ?: "")
}

fun foo(a: Any) {}

class Bar {
    operator fun invoke(a: Any) {}
}