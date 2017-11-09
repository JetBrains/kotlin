// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public class A {
    public static void invokeLater(Runnable doRun) {
    }
}

// FILE: 1.kt

fun <T> foo(t: T, x: (() -> Unit) -> Unit) {}

fun <T> bar(s: T) {}
fun <T> complex(t: T, f: (T) -> Unit) {}

fun test1() {
    foo(1, A::invokeLater)
    foo(1, ::bar)

    complex(1, ::bar)
}

fun <R> test2(x: R) {
    foo(x, A::invokeLater)
    foo(x, ::bar)

    complex(x, ::bar)
}