// !LANGUAGE: +NewInference

// NB new inference doesn't really work with old JVM back-end.

// WITH_JDK
// FILE: samConversionsWithSmartCasts.kt

fun test1(a: () -> Unit) {
    if (a is Runnable) {
        J.runStatic(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}

fun test2(a: () -> Unit) {
    if (a is Runnable) {
        J().run1(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}

fun test3(a: () -> Unit) {
    if (a is Runnable) {
        J().run2(<!DEBUG_INFO_SMARTCAST!>a<!>, <!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}

fun test4(a: () -> Unit, b: () -> Unit) {
    if (a is Runnable) {
        J().run2(a, b)
    }
}

fun test5(a: Any) {
    if (a is Runnable) {
        J().run1(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}

fun test5x(a: Any) {
    if (a is Runnable) {
        a <!UNCHECKED_CAST!>as () -> Unit<!>
        J().run1(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}

fun test6(a: Any) {
    a <!UNCHECKED_CAST!>as () -> Unit<!>
    J().run1(<!DEBUG_INFO_SMARTCAST!>a<!>)
}

fun test7(a: (Int) -> Int) {
    a <!UNCHECKED_CAST!>as () -> Unit<!>
    J().run1(<!DEBUG_INFO_SMARTCAST!>a<!>)
}

fun test8(a: () -> Unit) {
    J().run1(J.id(a))
}

fun test9() {
    J().run1(::test9)
}

// FILE: J.java
public class J {
    public static void runStatic(Runnable r) {}

    public void run1(Runnable r) {}

    public void run2(Runnable r1, Runnable r2) {}

    public static <T> T id(T x) { return x; }
}