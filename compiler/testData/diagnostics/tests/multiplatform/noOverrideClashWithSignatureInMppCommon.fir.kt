// LANGUAGE: +MultiPlatformProjects
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE
// WITH_STDLIB
// FULL_JDK

// MODULE: common
// FILE: Common.kt
expect abstract class B() {
    <!AMBIGUOUS_ACTUALS{JVM}!>fun get(index: Int): Char<!>
}

// MODULE: jvm()()(common)
// FILE: A.java
abstract public class A implements CharSequence {
    public final int length() {
        return 0;
    }

    public char charAt(int index) {
        return ' ';
    }

    public char get(int index) {
        return 'X';
    }
}

// FILE: main.kt

actual abstract class B : A() {
    // This would be: `ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE`
    // fun get(index: Int): Char = 'P'
}

fun foo(a: A, ka: B) {
    a.get(0)
    ka.get(0)
}

fun box() = "OK"
