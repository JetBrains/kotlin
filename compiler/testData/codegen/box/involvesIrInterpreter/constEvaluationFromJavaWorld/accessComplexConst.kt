// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_STDLIB
// FILE: Bar.java
package one.two;

public class Bar {
    public static final String BAR = MainKt.FOO + " JavaString";
}

// FILE: Main.kt
package one.two

const val FOO = "123".<!EVALUATED("123")!>trimIndent()<!>

const val BAZ = <!EVALUATED("123 JavaString KotlinString")!>Bar.BAR + " KotlinString"<!>

fun box(): String {
    return "OK"
}
