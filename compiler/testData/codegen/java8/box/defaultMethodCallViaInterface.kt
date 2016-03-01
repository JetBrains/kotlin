// FILE: Simple.java

public interface Simple {
    default String test(String s) {
        return s + "K";
    }

    static String testStatic(String s) {
        return s + "K";
    }
}

// FILE: main.kt

interface TestInterface : Simple {}
class Test : TestInterface {}

fun box(): String {
    val test = Test().test("O")
    if (test != "OK") return "fail $test"

    return Simple.testStatic("O")
}
