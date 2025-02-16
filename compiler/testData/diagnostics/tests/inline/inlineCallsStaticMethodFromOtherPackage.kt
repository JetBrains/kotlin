// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: Test.java

public class Test {
    protected static String testStatic() {
        return "OK";
    }
}

// FILE: test.kt

class Test2 {
    inline fun test() = Test.<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>testStatic<!>()
}

// FILE: test2.kt

package anotherPackage

import Test2

fun box() = Test2().test()
