// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: Test.java

class Test {
    static void foo(Comparable x) {}
}

// FILE: main.kt

fun main() {
    Test.foo(1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction */
