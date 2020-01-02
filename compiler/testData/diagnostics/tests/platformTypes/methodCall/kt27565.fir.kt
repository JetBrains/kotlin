// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-27565

// FILE: Runnable.java

public interface Runnable {
    void run();
}

// FILE: k.kt

fun fail() {
    <!AMBIGUITY!>foo<!>({ }, { })
    <!AMBIGUITY!>foo<!>(::bar, { })
}

fun foo(f: Runnable, selector: () -> Unit) {}
fun foo(func1: () -> Unit, func2: () -> Unit) {}

fun bar() {}