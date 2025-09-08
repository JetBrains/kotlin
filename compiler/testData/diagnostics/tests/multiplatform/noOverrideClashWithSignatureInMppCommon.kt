// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// FULL_JDK

// MODULE: common
// FILE: Common.kt
expect abstract class B() {
    fun get(index: Int): Char
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

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, javaFunction, javaType,
primaryConstructor, stringLiteral */
