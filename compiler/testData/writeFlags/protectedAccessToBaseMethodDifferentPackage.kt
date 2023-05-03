// LAMBDAS: CLASS
// IGNORE_BACKEND_K2: JVM_IR
// TARGET_BACKEND: JVM
// FILE: Base.java
public class Base {

    protected static String BASE_ONLY = "BASE";

    protected static String baseOnly() {
        return BASE_ONLY;
    }

    public static class Derived extends Base {

    }
}

// FILE: Kotlin.kt
package differentPackage;

import Base.Derived
import Base

fun <T> eval(f: () -> T) = f()

class Kotlin : Base.Derived() {
    fun doTest(): String {
        if (eval { Base.baseOnly() } != "BASE") return "fail 8"
        if (eval { baseOnly() } != "BASE") return "fail 10"
        return "FAIL"
    }
}
// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: differentPackage/Kotlin, access$baseOnly$s2063089
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: differentPackage/Kotlin, access$baseOnly$s-1074188803
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_SYNTHETIC

