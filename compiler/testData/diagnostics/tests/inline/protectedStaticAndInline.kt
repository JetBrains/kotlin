// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: First.java

public abstract class First {
    protected static String TEST = "O";

    protected static String test() {
        return "K";
    }
}

// FILE: Kotlin.kt

package anotherPackage

import First

class Test : First() {

    inline fun doTest(): String {
        return <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>TEST<!> + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>test<!>()
    }
}

fun box(): String {
    return Test().doTest()
}
