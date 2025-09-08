// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyInterface.java
public interface MyInterface {
    int MY_STATIC_FIELD = 1000;
}

// FILE: main.kt
fun main() {
    MyInterface.MY_STATIC_FIELD
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaProperty */
