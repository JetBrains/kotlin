// IGNORE_BACKEND_K2: JVM_IR
// TARGET_BACKEND: JVM
// LANGUAGE: -ProhibitVarargAsArrayAfterSamArgument
// WITH_JDK

// FILE: arrayAsVarargAfterSamArgument.kt
fun test(fn: () -> Unit, r: Runnable, arr: Array<String>) {
    Test.foo1({}, arr)
    Test.foo1({}, *arr)

    Test.foo1(fn, arr)
    Test.foo1(fn, *arr)
    Test.foo1(r, "")

    Test.foo1(fn, arr)
    Test.foo1(fn, *arr)
    Test.foo1(r, *arr)

    val i1 = Test({}, arr)
    val i2 = Test({}, *arr)
    val i3 = Test({}, {}, arr)
    val i4 = Test(r, {}, "")
    val i5 = Test({}, {}, *arr)
    val i6 = Test(r, {}, *arr)

    i1.foo2({}, {}, arr)
    i1.foo2(r, {}, "")
    i1.foo2({}, {}, *arr)
    i1.foo2(r, {}, *arr)
}

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
