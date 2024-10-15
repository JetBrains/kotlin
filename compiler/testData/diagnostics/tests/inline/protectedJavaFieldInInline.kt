// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: JavaClass.java

public class JavaClass {

    protected String FIELD = "OK";

}

// FILE: Kotlin.kt

package test

import JavaClass

class B : JavaClass() {
    inline fun bar() = <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>FIELD<!>
}

fun box() = B().bar()
