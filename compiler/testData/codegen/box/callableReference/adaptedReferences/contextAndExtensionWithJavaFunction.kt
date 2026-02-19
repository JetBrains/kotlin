// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
import org.C;

public class JavaClass {
    public static String foo(C a, C b, C c) { return a.foo() + b.foo() + c.foo(); }
}

// FILE: test.kt
package org
import JavaClass

class C(var a: String) {
    fun foo(): String { return a }
}

fun bar(x: context(C) C.(C) -> String): String {
    with(C("context ")) {
        return C("extension ").x(C("value"))
    }
}

fun box(): String {
    if (bar(JavaClass::foo) == "context extension value") return "OK"
    return "NOK"
}