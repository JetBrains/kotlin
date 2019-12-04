// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions
// IGNORE_BACKEND: JS, JS_IR
// SKIP_TXT

// FILE: Test.java
public class Test {
    public static String foo1(Runnable r, String... strs) {
        return null;
    }
    public String foo2(Runnable r, Runnable r, String... strs) {
        return null;
    }
    public Test(Runnable r, String... strs) {}
    public Test(Runnable r, Runnable r, String... strs) {}
}

// FILE: main.kt
fun main(x2: Runnable) {
    val x1 = {}
    val x3 = arrayOf<String>()

    Test.foo1({}, arrayOf())
    Test.foo1(<!TYPE_MISMATCH!>{}<!>, *arrayOf())
    Test.foo1(<!TYPE_MISMATCH!>{}<!>, *x3)
    Test.foo1({}, arrayOf(""))

    Test.foo1(x1, arrayOf())
    Test.foo1(<!TYPE_MISMATCH!>x1<!>, *arrayOf())
    Test.foo1(x2, <!TYPE_MISMATCH!>arrayOf()<!>)
    Test.foo1(x2, *arrayOf())

    Test.foo1(x1, x3)
    Test.foo1(<!TYPE_MISMATCH!>x1<!>, *x3)
    Test.foo1(x2, <!TYPE_MISMATCH!>arrayOf("")<!>)
    Test.foo1(x2, *arrayOf(""))

    val i1 = Test({}, arrayOf())
    val i2 = <!NONE_APPLICABLE!>Test<!>({}, *arrayOf())
    val i3 = Test({}, x3)
    val i4 = Test({}, arrayOf(""))
    val i5 = <!NONE_APPLICABLE!>Test<!>({}, {}, *arrayOf(""))
    val i6 = Test({}, {}, arrayOf())

    i1.foo2({}, {}, arrayOf())
    i1.foo2(<!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>, *arrayOf())
    i1.foo2(<!TYPE_MISMATCH!>{}<!>, x2, <!TYPE_MISMATCH!>arrayOf()<!>)
    i1.foo2(x2, <!TYPE_MISMATCH!>{}<!>, *arrayOf())

    i1.foo2({}, {}, arrayOf(""))
    i1.foo2(<!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>, *x3)
    i1.foo2(<!TYPE_MISMATCH!>{}<!>, x2, <!TYPE_MISMATCH!>x3<!>)
    i1.foo2(x2, <!TYPE_MISMATCH!>{}<!>, *arrayOf(""))
}