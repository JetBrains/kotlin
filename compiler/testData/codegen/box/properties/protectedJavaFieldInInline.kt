// !LANGUAGE: -ProhibitProtectedCallFromInline
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: JavaClass.java

public class JavaClass {

    protected String FIELD = "OK";

}

// FILE: Kotlin.kt

package test

import JavaClass

class B : JavaClass() {
    inline fun bar() = FIELD
}

fun box() = B().bar()
