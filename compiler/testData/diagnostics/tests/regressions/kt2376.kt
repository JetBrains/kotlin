// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: main.kt
//KT-2376 java.lang.Number should be visible in Kotlin as kotlin.Number
fun main() {
    Test().number(5)
}

// FILE: Test.java
public class Test {
    void number(Number n){}
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, javaType */
