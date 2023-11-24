// !LANGUAGE: -ProhibitVarargAsArrayAfterSamArgument
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// FIR status: don't support legacy feature
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
    Test.foo1({}, *arrayOf())
    Test.foo1({}, x3)
    Test.foo1({}, *arrayOf(""))

    Test.foo1(x1, arrayOf())
    Test.foo1(x1, *arrayOf())
    Test.foo1(x2, *arrayOf())

    Test.foo1(x1, x3)
    Test.foo1(x1, *x3)
    Test.foo1(x2, *arrayOf(""))

    val i1 = Test({}, arrayOf())
    val i2 = Test({}, *arrayOf())
    val i3 = Test({}, x3)
    val i4 = Test({}, arrayOf(""))
    val i5 = Test({}, {}, *arrayOf(""))
    val i6 = Test({}, {}, arrayOf())

    i1.foo2({}, {}, arrayOf())
    i2.foo2({}, {}, *arrayOf())
    i3.foo2(x2, {}, *arrayOf())

    i4.foo2({}, {}, arrayOf(""))
    i5.foo2({}, {}, *x3)
    i6.foo2(x2, {}, *arrayOf(""))

    return "OK"
}
