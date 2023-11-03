// !LANGUAGE: +SamConversionPerArgument +ProhibitVarargAsArrayAfterSamArgument
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
    foo(1, A::<!INAPPLICABLE_CANDIDATE!>invokeLater<!>) // KT-24507 SAM conversion accidentally applied to callable reference and incorrectly handled via BE
    foo(1, ::bar)

    complex(1, ::bar)
}

fun <R> test2(x: R) {
    foo(x, A::<!INAPPLICABLE_CANDIDATE!>invokeLater<!>) // KT-24507 SAM conversion accidentally applied to callable reference and incorrectly handled via BE
    foo(x, ::bar)

    complex(x, ::bar)
}
