// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-55548
// ISSUE: KT-9182

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    public static void foo(int x, Object y) {}
    public static void foo(Integer x, String y) {}

    public static void bar(int x, Object y) {}
    public static String bar(@NotNull Integer x, String y) {}
}

// FILE: main.kt
fun main() {
    J.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, "hello")
    J.bar(1, "hello").length
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, stringLiteral */
