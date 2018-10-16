// TARGET_BACKEND: JVM
// FILE: Simple.java

interface Simple {
    default String test(String s) {
        return s + "K";
    }

    static String testStatic(String s) {
        return s + "K";
    }
}

// FILE: main.kt
// JVM_TARGET: 1.8
class Test : Simple {}

fun box(): String {
    val test = Test().test("O")
    if (test != "OK") return "fail $test"

    return Simple.testStatic("O")
}
