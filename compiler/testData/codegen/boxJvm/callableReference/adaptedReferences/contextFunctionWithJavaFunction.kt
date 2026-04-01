// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
import org.C;

public class JavaClass {
    public static String foo(C x) { return x.foo(); }
}

// FILE: test.kt
package org
import JavaClass

class C(var a: String) {
    fun foo(): String { return a }
}

fun bar(x: context(C) () -> String): String {
    with(C("OK")) {
        return x()
    }
}

fun box(): String {
    return bar(JavaClass::foo)
}