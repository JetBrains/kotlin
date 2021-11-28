// TARGET_BACKEND: JVM
// FILE: test/J.java
package test;

public class J {
    String packagePrivate = "OK";
}

// FILE: test/main.kt
package test

object O {
    fun box() = privateInline()

    // no accessor needed - the only call site is in the same package
    private inline fun privateInline(): String = J().packagePrivate
}

fun box() = O.box()