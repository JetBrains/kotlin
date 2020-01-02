// IGNORE_BACKEND_FIR: JVM_IR
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

class Kotlin : Base.Derived() {
    fun doTest(): String {
        if ({ Base.baseOnly() }() != "BASE") return "fail 8"
        if ({ baseOnly() }() != "BASE") return "fail 10"
        return "FAIL"
    }
}
// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Kotlin, access$baseOnly$s2063089
// ABSENT: True