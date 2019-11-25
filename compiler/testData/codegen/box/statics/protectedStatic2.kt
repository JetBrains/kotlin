// IGNORE_BACKEND_FIR: JVM_IR
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

class Kotlin : Base.Derived() {
    fun doTest(): String {

        if ({ TEST }() != "DERIVED") return "fail 1"
        if ({ test() }() != "DERIVED") return "fail 2"

        if ({ Derived.TEST }() != "DERIVED") return "fail 3"
        if ({ Derived.test() }() != "DERIVED") return "fail 4"

        if ({ Base.TEST }() != "BASE") return "fail 5"
        if ({ Base.test() }() != "BASE") return "fail 6"


        if ({ Base.BASE_ONLY }() != "BASE") return "fail 7"
        if ({ Base.baseOnly() }() != "BASE") return "fail 8"

        if ({ BASE_ONLY }() != "BASE") return "fail 9"
        if ({ baseOnly() }() != "BASE") return "fail 10"

        return "OK"
    }
}

fun box(): String {
    return Kotlin().doTest()
}
