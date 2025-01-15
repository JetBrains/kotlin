// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
import org.A;

public class JavaClass {
    public String test() {
        A.Companion.foo("");
        A.Companion.getBar("");
        return A.foo("O") + A.getBar("K");
    }
}

// FILE: test.kt
package org
import JavaClass

class A {
    companion object {
        context(c: String)
        @JvmStatic
        fun foo(): String {
            return c
        }

        context(c: String)
        @JvmStatic
        val bar: String
            get() = c
    }
}

fun box(): String {
    return JavaClass().test()
}