// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
import org.KotlinInterface;

public class JavaClass implements KotlinInterface {
    @Override
    public String bar(String a, String b, String c) {
        return a + b + c;
    }
}

// FILE: test.kt
package org
import JavaClass

interface KotlinInterface {
    context(a: String)
    fun String.bar(b: String): String
}

fun box(): String {
    with("context ") {
        with(JavaClass()) {
            if ("extension ".bar("value") == "context extension value") return "OK"
            return "NOK"
        }
    }
}