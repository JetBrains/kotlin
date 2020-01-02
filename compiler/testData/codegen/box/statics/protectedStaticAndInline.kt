// !LANGUAGE: -ProhibitProtectedCallFromInline
// TARGET_BACKEND: JVM

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
        return TEST + test()
    }
}

fun box(): String {
    return Test().doTest()
}
