// LANGUAGE: -ProhibitProtectedCallFromInline
// TARGET_BACKEND: JVM
// The non-IR backend attempts to call a non-existent accessor in class Test.
// IGNORE_BACKEND: JVM

// FILE: Test.java

public class Test {
    protected static String testStatic() {
        return "OK";
    }
}

// FILE: test.kt

class Test2 {
    inline fun test() = Test.testStatic()
}

// FILE: test2.kt

package anotherPackage

import Test2

fun box() = Test2().test()
