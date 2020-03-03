// TARGET_BACKEND: JVM
// FILE: varModifiedAfterCheck.kt
fun implicitCheck(s: String) {}

fun test1() {
    var a = J.foo()
    val b = a
    a = J.bar()
    if (b != null) {
        if (a != null) {
            throw AssertionError("a: $a")
        }
    }
}

fun test2() {
    var a = J.foo()
    val b = a
    if (b != null) {
        a = J.bar()
        if (a != null) {
            throw AssertionError("a: $a")
        }
    }
}

fun test3() {
    var a = J.foo()
    val b = a
    a = J.bar()
    implicitCheck(b)
    if (a != null) {
        throw AssertionError("a: $a")
    }
}

fun test4() {
    var a = J.foo()
    val b = a
    implicitCheck(b)
    a = J.bar()
    if (a != null) {
        throw AssertionError("a: $a")
    }
}

fun box(): String {
    test1()
    test2()
    test3()
    test4()
    return "OK"
}

// FILE: J.java
public class J {
    public static String foo() { return ""; }
    public static String bar() { return null; }
}