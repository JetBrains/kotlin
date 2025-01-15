// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
import org.KotlinInterface;

public class JavaClass implements KotlinInterface {
    @Override
    public String foo(String a) {
        return a;
    }
}

// FILE: test.kt
package org
import JavaClass

interface KotlinInterface {
    context(a: String)
    fun foo(): String
}

fun box(): String {
    with("OK") {
        return JavaClass().foo()
    }
}