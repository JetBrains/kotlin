// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: Test.java
public class Test {
    public static String foo1(Runnable r, String... strs) {
        return null;
    }
    public String foo2(Runnable r1, Runnable r2, String... strs) {
        return null;
    }
    public Test(Runnable r, String... strs) {}
    public Test(Runnable r1, Runnable r2, String... strs) {}
}

// FILE: main.kt
fun box(): String {
    val x1 = {}
    val x2: Runnable = Runnable { }
    val x3 = arrayOf<String>()

    Test.foo1({}, arrayOf())
    Test.foo1({}, arrayOf(""))

    Test.foo1(x1, arrayOf())
    Test.foo1(x2, *arrayOf())

    Test.foo1(x1, x3)
    Test.foo1(x2, *arrayOf(""))

    val i1 = Test({}, arrayOf())
    val i3 = Test({}, x3)
    val i4 = Test({}, arrayOf(""))
    val i6 = Test({}, {}, arrayOf())

    i1.foo2({}, {}, arrayOf())
    i1.foo2({}, {}, arrayOf(""))

    return "OK"
}