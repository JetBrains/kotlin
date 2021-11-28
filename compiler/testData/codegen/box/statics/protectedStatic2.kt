// TARGET_BACKEND: JVM

// FILE: Base.java

public class Base {

    protected static String BASE_ONLY = "BASE";

    protected static String baseOnly() {
        return BASE_ONLY;
    }

    protected static String TEST = "BASE";

    protected static String test() {
        return TEST;
    }

    public static class Derived extends Base {
        protected static String TEST = "DERIVED";

        protected static String test() {
            return TEST;
        }
    }
}

// FILE: Kotlin.kt

package anotherPackage

import Base.Derived
import Base

fun <T> eval(fn: () -> T) = fn()

class Kotlin : Base.Derived() {
    fun doTest(): String {

        if (eval { TEST } != "DERIVED") return "fail 1"
        if (eval { test() } != "DERIVED") return "fail 2"

        if (eval { Derived.TEST } != "DERIVED") return "fail 3"
        if (eval { Derived.test() } != "DERIVED") return "fail 4"

        if (eval { Base.TEST } != "BASE") return "fail 5"
        if (eval { Base.test() } != "BASE") return "fail 6"

        if (eval { Base.BASE_ONLY } != "BASE") return "fail 7"
        if (eval { Base.baseOnly() } != "BASE") return "fail 8"

        if (eval { BASE_ONLY } != "BASE") return "fail 9"
        if (eval { baseOnly() } != "BASE") return "fail 10"

        return "OK"
    }
}

fun box(): String {
    return Kotlin().doTest()
}
